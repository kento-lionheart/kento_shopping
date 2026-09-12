package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.StockRequest;
import com.e_commerce.kento_shopping.dto.request.admin.ProductCreateRequest;
import com.e_commerce.kento_shopping.dto.request.admin.ProductRequest;
import com.e_commerce.kento_shopping.dto.response.ProductResponse;
import com.e_commerce.kento_shopping.entity.Category;
import com.e_commerce.kento_shopping.entity.Inventory;
import com.e_commerce.kento_shopping.entity.Product;
import com.e_commerce.kento_shopping.enums.OrderStatus;
import com.e_commerce.kento_shopping.exception.CategoryNotFoundException;
import com.e_commerce.kento_shopping.exception.ProductNotFoundException;
import com.e_commerce.kento_shopping.repository.CategoryRepository;
import com.e_commerce.kento_shopping.repository.InventoryRepository;
import com.e_commerce.kento_shopping.repository.OrderItemRepository;
import com.e_commerce.kento_shopping.repository.ProductRepository;
import com.e_commerce.kento_shopping.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final Pageable PAGEABLE =
            PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"));

    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.SHIPPED);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Captor
    private ArgumentCaptor<Inventory> inventoryCaptor;

    @Captor
    private ArgumentCaptor<Specification<Product>> specCaptor;

    private Category category(Long id, String name) {
        Category category = Category.builder().name(name).build();
        category.setId(id);
        return category;
    }

    private Product product(Long id, String name, BigDecimal price, Category category, Integer quantity) {
        Product product = Product.builder()
                .name(name)
                .description(name + " description")
                .price(price)
                .imageUrl("/images/products/electronics/" + id + ".png")
                .category(category)
                .build();
        product.setId(id);
        if (quantity != null) {
            product.setInventory(Inventory.builder().id(id).product(product).quantity(quantity).build());
        }
        return product;
    }

    private ProductCreateRequest createRequest(Long categoryId, Integer quantity) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("iPhone 15 Pro");
        request.setDescription("A titanium phone");
        request.setPrice(new BigDecimal("29990000"));
        request.setImageUrl("/images/products/electronics/iphone15pro.png");
        request.setCategoryId(categoryId);
        request.setQuantity(quantity);
        return request;
    }

    private ProductRequest updateRequest(Long categoryId) {
        ProductRequest request = new ProductRequest();
        request.setName("iPhone 15 Pro Max");
        request.setDescription("A bigger titanium phone");
        request.setPrice(new BigDecimal("34990000"));
        request.setImageUrl("/images/products/electronics/iphone15promax.png");
        request.setCategoryId(categoryId);
        return request;
    }

    private StockRequest stockRequest(Integer quantity) {
        StockRequest request = new StockRequest();
        request.setQuantity(quantity);
        return request;
    }

    @Test
    void createProductPersistsTheProductWithTheResolvedCategory() {
        Category electronics = category(3L, "Electronics");
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(electronics));

        productService.createProduct(createRequest(3L, 25));

        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(saved.getDescription()).isEqualTo("A titanium phone");
        assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("29990000"));
        assertThat(saved.getImageUrl()).isEqualTo("/images/products/electronics/iphone15pro.png");
        assertThat(saved.getCategory()).isSameAs(electronics);
    }

    @Test
    void createProductCreatesAnInventoryRowForTheNewProduct() {
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, "Electronics")));

        productService.createProduct(createRequest(3L, 25));

        verify(productRepository).save(productCaptor.capture());
        verify(inventoryRepository).save(inventoryCaptor.capture());
        Inventory inventory = inventoryCaptor.getValue();
        assertThat(inventory.getQuantity()).isEqualTo(25);
        assertThat(inventory.getProduct()).isSameAs(productCaptor.getValue());
    }

    @Test
    void createProductRejectsAnUnknownCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(createRequest(99L, 25)))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found");

        verifyNoInteractions(productRepository, inventoryRepository);
    }

    @Test
    void updateProductMutatesTheManagedEntityInPlace() {
        Category books = category(4L, "Books");
        Product existing = product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 5);
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(books));
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.updateProduct(1L, updateRequest(4L));

        assertThat(existing.getName()).isEqualTo("iPhone 15 Pro Max");
        assertThat(existing.getDescription()).isEqualTo("A bigger titanium phone");
        assertThat(existing.getPrice()).isEqualByComparingTo(new BigDecimal("34990000"));
        assertThat(existing.getImageUrl()).isEqualTo("/images/products/electronics/iphone15promax.png");
        assertThat(existing.getCategory()).isSameAs(books);
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProductThrowsWhenTheProductDoesNotExist() {
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category(4L, "Books")));
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(404L, updateRequest(4L)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void updateProductRejectsAnUnknownCategoryBeforeLookingUpTheProduct() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest(99L)))
                .isInstanceOf(CategoryNotFoundException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void deleteProductRemovesAProductThatHasNoActiveOrders() {
        Product existing = product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderItemRepository.existsByProductAndOrderStatusIn(existing, ACTIVE_STATUSES)).thenReturn(false);

        productService.deleteProduct(1L);

        verify(productRepository).delete(existing);
    }

    @Test
    void deleteProductRefusesWhenPendingPaidOrShippedOrdersReferenceIt() {
        Product existing = product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderItemRepository.existsByProductAndOrderStatusIn(existing, ACTIVE_STATUSES)).thenReturn(true);

        assertThatThrownBy(() -> productService.deleteProduct(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot delete product with active orders");

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProductThrowsWhenTheProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(404L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found");

        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void getProductByIdReturnsTheMappedProductWithItsStockStatus() {
        Product existing = product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("29990000"));
        assertThat(response.getCategoryName()).isEqualTo("Electronics");
        assertThat(response.getQuantity()).isEqualTo(5);
        assertThat(response.getStockStatus()).isEqualTo("IN_STOCK");
    }

    @Test
    void getProductByIdReportsOutOfStockWhenTheInventoryIsEmpty() {
        Product existing = product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getQuantity()).isZero();
        assertThat(response.getStockStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void getProductByIdTreatsAMissingInventoryRowAsOutOfStock() {
        Product existing = product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getQuantity()).isZero();
        assertThat(response.getStockStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void getProductByIdThrowsWhenTheProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(404L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void updateStockSetsTheInventoryQuantity() {
        Product existing = product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 5);
        Inventory inventory = existing.getInventory();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.findByProduct(existing)).thenReturn(Optional.of(inventory));

        productService.updateStock(1L, stockRequest(42));

        assertThat(inventory.getQuantity()).isEqualTo(42);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void updateStockThrowsWhenTheProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateStock(404L, stockRequest(42)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found");

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void updateStockThrowsProductNotFoundWhenTheInventoryRowIsMissing() {
        Product existing = product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.findByProduct(existing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateStock(1L, stockRequest(42)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Inventory not found the product");
    }

    @Test
    void updateStockAcceptsANegativeQuantityBecauseTheOnlyBoundIsBeanValidationOnTheRequest() {
        Product existing = product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 5);
        Inventory inventory = existing.getInventory();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.findByProduct(existing)).thenReturn(Optional.of(inventory));

        productService.updateStock(1L, stockRequest(-7));

        assertThat(inventory.getQuantity()).isEqualTo(-7);
    }

    @Test
    void getProductsBuildsASpecificationWhenOnlyASearchTermIsGiven() {
        Page<Product> page = new PageImpl<>(List.of(
                product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 5)),
                PAGEABLE, 1);
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), eq(PAGEABLE)))
                .thenReturn(page);

        Page<ProductResponse> result = productService.getProducts("iphone", null, PAGEABLE);

        verify(productRepository).findAll(specCaptor.capture(), eq(PAGEABLE));
        assertThat(specCaptor.getValue()).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement()
                .satisfies(response -> {
                    assertThat(response.getName()).isEqualTo("iPhone 15 Pro");
                    assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("29990000"));
                    assertThat(response.getCategoryName()).isEqualTo("Electronics");
                    assertThat(response.getQuantity()).isEqualTo(5);
                    assertThat(response.getStockStatus()).isEqualTo("IN_STOCK");
                });
    }

    @Test
    void getProductsBuildsASpecificationWhenOnlyACategoryIsGiven() {
        Page<Product> page = new PageImpl<>(List.of(
                product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 5)),
                PAGEABLE, 1);
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), eq(PAGEABLE)))
                .thenReturn(page);

        Page<ProductResponse> result = productService.getProducts(null, 3L, PAGEABLE);

        verify(productRepository).findAll(specCaptor.capture(), eq(PAGEABLE));
        assertThat(specCaptor.getValue()).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getProductsBuildsASpecificationWhenBothSearchAndCategoryAreGiven() {
        Page<Product> page = new PageImpl<>(List.of(
                product(1L, "iPhone 15 Pro", new BigDecimal("29990000"), category(3L, "Electronics"), 5)),
                PAGEABLE, 1);
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), eq(PAGEABLE)))
                .thenReturn(page);

        Page<ProductResponse> result = productService.getProducts("  iPhone  ", 3L, PAGEABLE);

        verify(productRepository).findAll(specCaptor.capture(), eq(PAGEABLE));
        assertThat(specCaptor.getValue()).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getProductsStillPassesASpecificationWhenNoFilterIsGiven() {
        Page<Product> page = new PageImpl<>(List.of(), PAGEABLE, 0);
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), eq(PAGEABLE)))
                .thenReturn(page);

        Page<ProductResponse> result = productService.getProducts("   ", null, PAGEABLE);

        verify(productRepository).findAll(specCaptor.capture(), eq(PAGEABLE));
        assertThat(specCaptor.getValue()).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPageable()).isEqualTo(PAGEABLE);
    }
}
