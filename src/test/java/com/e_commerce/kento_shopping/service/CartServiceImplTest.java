package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.CartItemRequest;
import com.e_commerce.kento_shopping.dto.request.UpdateCartItemQuantityRequest;
import com.e_commerce.kento_shopping.dto.response.CartItemResponse;
import com.e_commerce.kento_shopping.dto.response.CartResponse;
import com.e_commerce.kento_shopping.entity.Cart;
import com.e_commerce.kento_shopping.entity.CartItem;
import com.e_commerce.kento_shopping.entity.Inventory;
import com.e_commerce.kento_shopping.entity.Product;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.exception.CartItemNotFoundException;
import com.e_commerce.kento_shopping.exception.CartNotFoundException;
import com.e_commerce.kento_shopping.exception.InsufficientStockException;
import com.e_commerce.kento_shopping.exception.ProductNotFoundException;
import com.e_commerce.kento_shopping.repository.CartRepository;
import com.e_commerce.kento_shopping.repository.ProductRepository;
import com.e_commerce.kento_shopping.repository.UserRepository;
import com.e_commerce.kento_shopping.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;

    @BeforeEach
    void setUp() {
        user = newUser(7L);
    }

    @Test
    void getOrCreateReturnsExistingCart() {
        Cart cart = newCart(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        Cart result = cartService.getOrCreate(user);

        assertThat(result).isSameAs(cart);
        verify(cartRepository, never()).saveAndFlush(any(Cart.class));
    }

    @Test
    void getOrCreateCreatesCartWhenAbsent() {
        Cart created = newCart(user);
        User managed = newUser(7L);
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(7L)).thenReturn(managed);
        when(cartRepository.saveAndFlush(any(Cart.class))).thenReturn(created);

        Cart result = cartService.getOrCreate(user);

        assertThat(result).isSameAs(created);
        verify(userRepository).getReferenceById(7L);
        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(managed);
    }

    @Test
    void getOrCreateRereadsCartWhenConcurrentInsertWins() {
        Cart winner = newCart(user);
        when(cartRepository.findByUser(user))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(cartRepository.saveAndFlush(any(Cart.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        Cart result = cartService.getOrCreate(user);

        assertThat(result).isSameAs(winner);
    }

    @Test
    void getOrCreateThrowsWhenRereadFindsNothingAfterConstraintViolation() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(cartRepository.saveAndFlush(any(Cart.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> cartService.getOrCreate(user))
                .isInstanceOf(CartNotFoundException.class)
                .hasMessage("Cart not found");
    }

    @Test
    void addItemAddsNewLineWhenProductNotInCart() {
        Cart cart = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", 10);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        CartResponse response = cartService.addItem(user, cartItemRequest(3L, 2));

        assertThat(cart.getItems()).hasSize(1);
        CartItem line = cart.getItems().get(0);
        assertThat(line.getProduct()).isSameAs(product);
        assertThat(line.getQuantity()).isEqualTo(2);
        assertThat(line.getCart()).isSameAs(cart);
        assertThat(line.getId().getCartId()).isEqualTo(cart.getId());
        assertThat(line.getId().getProductId()).isEqualTo(3L);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItemCount()).isEqualTo(2);
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("19.98"));
    }

    @Test
    void addItemIncrementsQuantityWhenProductAlreadyInCart() {
        Cart cart = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", 10);
        addLine(cart, product, 2);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.addItem(user, cartItemRequest(3L, 3));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(response.getItemCount()).isEqualTo(5);
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("49.95"));
    }

    @Test
    void addItemThrowsWhenProductDoesNotExist() {
        Cart cart = newCart(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(user, cartItemRequest(404L, 1)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found");
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void addItemThrowsWhenQuantityExceedsStock() {
        Cart cart = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", 2);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(user, cartItemRequest(3L, 3)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock");
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void addItemThrowsWhenProductHasNoInventoryRow() {
        Cart cart = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", null);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(user, cartItemRequest(3L, 1)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock");
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void addItemAcceptsQuantityExactlyEqualToStock() {
        Cart cart = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", 4);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        cartService.addItem(user, cartItemRequest(3L, 4));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(4);
    }

    @Test
    void addItemCreatesCartWhenCustomerHasNoneYet() {
        Cart created = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", 10);
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(cartRepository.saveAndFlush(any(Cart.class))).thenReturn(created);
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        CartResponse response = cartService.addItem(user, cartItemRequest(3L, 1));

        assertThat(created.getItems()).hasSize(1);
        assertThat(response.getItemCount()).isEqualTo(1);
    }

    @Test
    void updateItemChangesQuantity() {
        Cart cart = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", 10);
        addLine(cart, product, 2);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.updateItem(user, 3L, updateQuantityRequest(6));

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(6);
        assertThat(response.getItemCount()).isEqualTo(6);
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("59.94"));
    }

    @Test
    void updateItemThrowsWhenQuantityExceedsStock() {
        Cart cart = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", 5);
        addLine(cart, product, 2);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem(user, 3L, updateQuantityRequest(6)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock");
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void updateItemThrowsWhenProductHasNoInventoryRow() {
        Cart cart = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", null);
        addLine(cart, product, 2);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem(user, 3L, updateQuantityRequest(1)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock");
    }

    @Test
    void updateItemStoresZeroQuantityWithoutRemovingTheLine() {
        Cart cart = newCart(user);
        Product product = newProduct(3L, "Mouse", "9.99", 5);
        addLine(cart, product, 2);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.updateItem(user, 3L, updateQuantityRequest(0));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isZero();
        assertThat(response.getItemCount()).isZero();
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateItemThrowsWhenProductNotInCart() {
        Cart cart = newCart(user);
        addLine(cart, newProduct(3L, "Mouse", "9.99", 10), 1);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem(user, 99L, updateQuantityRequest(1)))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessage("Item not in cart");
    }

    @Test
    void updateItemThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItem(user, 3L, updateQuantityRequest(1)))
                .isInstanceOf(CartNotFoundException.class)
                .hasMessage("Cart not found");
        verify(cartRepository, never()).saveAndFlush(any(Cart.class));
    }

    @Test
    void removeItemDropsTheLine() {
        Cart cart = newCart(user);
        Product kept = newProduct(3L, "Mouse", "9.99", 10);
        Product removed = newProduct(4L, "Keyboard", "20.00", 10);
        addLine(cart, kept, 1);
        addLine(cart, removed, 2);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.removeItem(user, 4L);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProduct()).isSameAs(kept);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("9.99"));
    }

    @Test
    void removeItemThrowsWhenProductNotInCart() {
        Cart cart = newCart(user);
        addLine(cart, newProduct(3L, "Mouse", "9.99", 10), 1);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem(user, 99L))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessage("Item not in cart");
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void removeItemThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(user, 3L))
                .isInstanceOf(CartNotFoundException.class)
                .hasMessage("Cart not found");
    }

    @Test
    void clearCartEmptiesTheItems() {
        Cart cart = newCart(user);
        addLine(cart, newProduct(3L, "Mouse", "9.99", 10), 1);
        addLine(cart, newProduct(4L, "Keyboard", "20.00", 10), 2);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart(user);

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getItemCount()).isZero();
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCartComputesLineSubTotalsAndCartTotal() {
        Cart cart = newCart(user);
        Product mouse = newProduct(3L, "Mouse", "9.99", 10);
        Product keyboard = newProduct(4L, "Keyboard", "199.50", 10);
        addLine(cart, mouse, 3);
        addLine(cart, keyboard, 2);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(user);

        assertThat(response.getItems()).hasSize(2);
        CartItemResponse first = response.getItems().get(0);
        assertThat(first.getProductId()).isEqualTo(3L);
        assertThat(first.getProductName()).isEqualTo("Mouse");
        assertThat(first.getImageUrl()).isEqualTo("/images/products/3.png");
        assertThat(first.getPrice()).isEqualByComparingTo(new BigDecimal("9.99"));
        assertThat(first.getSubTotal()).isEqualByComparingTo(new BigDecimal("29.97"));
        assertThat(response.getItems().get(1).getSubTotal()).isEqualByComparingTo(new BigDecimal("399.00"));
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("428.97"));
        assertThat(response.getItemCount()).isEqualTo(5);
    }

    @Test
    void getCartMarksLineOutOfStockWhenInventoryIsExhausted() {
        Cart cart = newCart(user);
        Product soldOut = newProduct(3L, "Mouse", "9.99", 0);
        Product available = newProduct(4L, "Keyboard", "20.00", 5);
        addLine(cart, soldOut, 1);
        addLine(cart, available, 1);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(user);

        assertThat(response.getItems().get(0).getStockStatus()).isEqualTo("OUT_OF_STOCK");
        assertThat(response.getItems().get(1).getStockStatus()).isEqualTo("IN_STOCK");
    }

    @Test
    void getCartMarksLineOutOfStockWhenProductHasNoInventoryRow() {
        Cart cart = newCart(user);
        addLine(cart, newProduct(3L, "Mouse", "9.99", null), 1);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(user);

        assertThat(response.getItems().get(0).getStockStatus()).isEqualTo("OUT_OF_STOCK");
    }

    private User newUser(Long id) {
        User u = User.builder()
                .email("customer@kento.com")
                .password("secret")
                .fullName("Customer")
                .phoneNumber("0900000000")
                .build();
        u.setId(id);
        return u;
    }

    private Cart newCart(User owner) {
        Cart cart = Cart.builder().user(owner).build();
        cart.setId(owner.getId());
        return cart;
    }

    private Product newProduct(Long id, String name, String price, Integer stock) {
        Product product = Product.builder()
                .name(name)
                .price(new BigDecimal(price))
                .imageUrl("/images/products/" + id + ".png")
                .build();
        product.setId(id);
        if (stock != null) {
            product.setInventory(Inventory.builder()
                    .id(id)
                    .product(product)
                    .quantity(stock)
                    .build());
        }
        return product;
    }

    private void addLine(Cart cart, Product product, int quantity) {
        cart.getItems().add(CartItem.builder()
                .id(new CartItem.Id(cart.getId(), product.getId()))
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .build());
    }

    private CartItemRequest cartItemRequest(Long productId, Integer quantity) {
        CartItemRequest request = new CartItemRequest();
        ReflectionTestUtils.setField(request, "productId", productId);
        ReflectionTestUtils.setField(request, "quantity", quantity);
        return request;
    }

    private UpdateCartItemQuantityRequest updateQuantityRequest(Integer quantity) {
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        ReflectionTestUtils.setField(request, "quantity", quantity);
        return request;
    }
}
