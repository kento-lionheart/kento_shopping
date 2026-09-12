package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.CheckoutRequest;
import com.e_commerce.kento_shopping.dto.request.admin.UpdateOrderStatusRequest;
import com.e_commerce.kento_shopping.dto.response.AdminOrderSummaryResponse;
import com.e_commerce.kento_shopping.dto.response.OrderItemResponse;
import com.e_commerce.kento_shopping.dto.response.OrderResponse;
import com.e_commerce.kento_shopping.dto.response.OrderSummaryResponse;
import com.e_commerce.kento_shopping.entity.Cart;
import com.e_commerce.kento_shopping.entity.CartItem;
import com.e_commerce.kento_shopping.entity.Inventory;
import com.e_commerce.kento_shopping.entity.Order;
import com.e_commerce.kento_shopping.entity.OrderItem;
import com.e_commerce.kento_shopping.entity.Payment;
import com.e_commerce.kento_shopping.entity.Product;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.entity.Wallet;
import com.e_commerce.kento_shopping.enums.CoinTxType;
import com.e_commerce.kento_shopping.enums.OrderStatus;
import com.e_commerce.kento_shopping.enums.PaymentMethod;
import com.e_commerce.kento_shopping.enums.PaymentStatus;
import com.e_commerce.kento_shopping.exception.InsufficientBalanceException;
import com.e_commerce.kento_shopping.exception.InsufficientStockException;
import com.e_commerce.kento_shopping.exception.OrderNotFoundException;
import com.e_commerce.kento_shopping.exception.ResourceAccessDeniedException;
import com.e_commerce.kento_shopping.repository.OrderRepository;
import com.e_commerce.kento_shopping.repository.PaymentRepository;
import com.e_commerce.kento_shopping.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final BigDecimal SHIPPING_FEE = new BigDecimal("30000");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    @Captor
    private ArgumentCaptor<BigDecimal> amountCaptor;

    private record ItemSpec(String name, BigDecimal price, Integer stock, int quantity) {
    }

    private User user(Long id, String email) {
        User user = User.builder()
                .email(email)
                .password("encoded")
                .fullName("Nguyen Van An")
                .phoneNumber("0900000000")
                .build();
        user.setId(id);
        return user;
    }

    private Product product(Long id, ItemSpec spec) {
        Product product = Product.builder()
                .name(spec.name())
                .description(spec.name() + " description")
                .price(spec.price())
                .imageUrl("/images/products/electronics/" + id + ".png")
                .build();
        product.setId(id);
        if (spec.stock() != null) {
            Inventory inventory = Inventory.builder()
                    .id(id)
                    .product(product)
                    .quantity(spec.stock())
                    .build();
            product.setInventory(inventory);
        }
        return product;
    }

    private Cart cartWith(User owner, ItemSpec... specs) {
        Cart cart = Cart.builder().user(owner).build();
        cart.setId(owner.getId());
        long productId = 100L;
        for (ItemSpec spec : specs) {
            Product product = product(productId, spec);
            CartItem cartItem = CartItem.builder()
                    .id(new CartItem.Id(cart.getId(), product.getId()))
                    .cart(cart)
                    .product(product)
                    .quantity(spec.quantity())
                    .build();
            cart.getItems().add(cartItem);
            productId++;
        }
        return cart;
    }

    private Order orderWith(Long orderId, User owner, OrderStatus status, ItemSpec... specs) {
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        long productId = 200L;
        for (ItemSpec spec : specs) {
            Product product = product(productId, spec);
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .priceAtPurchase(product.getPrice())
                    .quantity(spec.quantity())
                    .build();
            orderItem.setId(productId);
            orderItems.add(orderItem);
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(spec.quantity())));
            productId++;
        }
        Order order = Order.builder()
                .user(owner)
                .subtotal(subtotal)
                .shippingFee(SHIPPING_FEE)
                .totalAmount(subtotal.add(SHIPPING_FEE))
                .status(status)
                .shipRecipientName("Nguyen Van An")
                .shipPhone("0900000000")
                .shipStreet("12 Le Loi")
                .shipWard("Ben Nghe")
                .shipDistrict("District 1")
                .shipCity("Ho Chi Minh")
                .shipPostalCode("700000")
                .build();
        order.setId(orderId);
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
        }
        return order;
    }

    private Payment payment(Order order, PaymentStatus status) {
        Payment payment = Payment.builder()
                .order(order)
                .method(PaymentMethod.COIN)
                .amount(order.getTotalAmount())
                .transactionId("tx-" + order.getId())
                .status(status)
                .paidAt(LocalDateTime.now())
                .build();
        payment.setId(order.getId());
        return payment;
    }

    private CheckoutRequest checkoutRequest() {
        CheckoutRequest request = new CheckoutRequest();
        ReflectionTestUtils.setField(request, "recipientName", "Tran Danh Kiet");
        ReflectionTestUtils.setField(request, "phone", "0912345678");
        ReflectionTestUtils.setField(request, "street", "68 Nguyen Hue");
        ReflectionTestUtils.setField(request, "ward", "Ben Nghe");
        ReflectionTestUtils.setField(request, "district", "District 1");
        ReflectionTestUtils.setField(request, "city", "Ho Chi Minh");
        ReflectionTestUtils.setField(request, "postalCode", "700000");
        return request;
    }

    private UpdateOrderStatusRequest updateStatusRequest(OrderStatus status) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        ReflectionTestUtils.setField(request, "status", status);
        return request;
    }

    private Wallet wallet(User owner, BigDecimal balance) {
        Wallet wallet = Wallet.builder().user(owner).balance(balance).build();
        wallet.setId(owner.getId());
        return wallet;
    }

    @Test
    void checkoutThrowsWhenCartIsEmpty() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        when(cartService.getOrCreate(customer)).thenReturn(cartWith(customer));

        assertThatThrownBy(() -> orderService.checkout(customer, checkoutRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart is empty");

        verifyNoInteractions(orderRepository);
    }

    @Test
    void checkoutThrowsWhenStockIsInsufficient() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Cart cart = cartWith(customer,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1),
                new ItemSpec("MacBook Air", new BigDecimal("25000000"), 1, 3));
        when(cartService.getOrCreate(customer)).thenReturn(cart);

        assertThatThrownBy(() -> orderService.checkout(customer, checkoutRequest()))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("MacBook Air has insufficient stock");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkoutThrowsWhenProductHasNoInventoryRow() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Cart cart = cartWith(customer,
                new ItemSpec("Ghost Product", new BigDecimal("100000"), null, 1));
        when(cartService.getOrCreate(customer)).thenReturn(cart);

        assertThatThrownBy(() -> orderService.checkout(customer, checkoutRequest()))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Ghost Product has insufficient stock");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkoutComputesSubtotalFlatShippingFeeAndTotalAmount() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Cart cart = cartWith(customer,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 2),
                new ItemSpec("AirPods Pro", new BigDecimal("5000000"), 10, 3));
        when(cartService.getOrCreate(customer)).thenReturn(cart);

        OrderResponse response = orderService.checkout(customer, checkoutRequest());

        assertThat(response.getSubTotal()).isEqualByComparingTo(new BigDecimal("75000000"));
        assertThat(response.getShippingFee()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("75030000"));
    }

    @Test
    void checkoutCreatesPendingOrderWithSnapshottedItems() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Cart cart = cartWith(customer,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 2),
                new ItemSpec("AirPods Pro", new BigDecimal("5000000"), 10, 3));
        when(cartService.getOrCreate(customer)).thenReturn(cart);

        OrderResponse response = orderService.checkout(customer, checkoutRequest());

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getItems()).hasSize(2);

        OrderItemResponse first = response.getItems().get(0);
        assertThat(first.getProductName()).isEqualTo("iPhone 15 Pro");
        assertThat(first.getQuantity()).isEqualTo(2);
        assertThat(first.getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("30000000"));
        assertThat(first.getSubTotal()).isEqualByComparingTo(new BigDecimal("60000000"));

        OrderItemResponse second = response.getItems().get(1);
        assertThat(second.getProductName()).isEqualTo("AirPods Pro");
        assertThat(second.getQuantity()).isEqualTo(3);
        assertThat(second.getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(second.getSubTotal()).isEqualByComparingTo(new BigDecimal("15000000"));
    }

    @Test
    void checkoutSavesOrderWithOneOrderItemPerCartItem() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Cart cart = cartWith(customer,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 2));
        when(cartService.getOrCreate(customer)).thenReturn(cart);
        Product product = cart.getItems().get(0).getProduct();

        orderService.checkout(customer, checkoutRequest());

        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getUser()).isSameAs(customer);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getItems()).hasSize(1);

        OrderItem item = saved.getItems().get(0);
        assertThat(item.getOrder()).isSameAs(saved);
        assertThat(item.getProduct()).isSameAs(product);
        assertThat(item.getProductName()).isEqualTo("iPhone 15 Pro");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("30000000"));
    }

    @Test
    void checkoutSnapshotsNameAndPriceSoLaterProductEditsDoNotRewriteHistory() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Cart cart = cartWith(customer,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        when(cartService.getOrCreate(customer)).thenReturn(cart);
        Product product = cart.getItems().get(0).getProduct();

        orderService.checkout(customer, checkoutRequest());

        verify(orderRepository).save(orderCaptor.capture());
        OrderItem item = orderCaptor.getValue().getItems().get(0);

        product.setName("iPhone 15 Pro Max");
        product.setPrice(new BigDecimal("40000000"));

        assertThat(item.getProductName()).isEqualTo("iPhone 15 Pro");
        assertThat(item.getPriceAtPurchase()).isEqualByComparingTo(new BigDecimal("30000000"));
    }

    @Test
    void checkoutDecrementsInventoryByOrderedQuantityForEachItem() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Cart cart = cartWith(customer,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 2),
                new ItemSpec("AirPods Pro", new BigDecimal("5000000"), 7, 3));
        when(cartService.getOrCreate(customer)).thenReturn(cart);
        Inventory first = cart.getItems().get(0).getProduct().getInventory();
        Inventory second = cart.getItems().get(1).getProduct().getInventory();

        orderService.checkout(customer, checkoutRequest());

        assertThat(first.getQuantity()).isEqualTo(8);
        assertThat(second.getQuantity()).isEqualTo(4);
    }

    @Test
    void checkoutClearsTheCart() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Cart cart = cartWith(customer,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 2));
        when(cartService.getOrCreate(customer)).thenReturn(cart);

        orderService.checkout(customer, checkoutRequest());

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void checkoutCopiesShippingAddressFieldsFromRequest() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Cart cart = cartWith(customer,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        when(cartService.getOrCreate(customer)).thenReturn(cart);

        OrderResponse response = orderService.checkout(customer, checkoutRequest());

        assertThat(response.getShipRecipientName()).isEqualTo("Tran Danh Kiet");
        assertThat(response.getShipPhone()).isEqualTo("0912345678");
        assertThat(response.getShipStreet()).isEqualTo("68 Nguyen Hue");
        assertThat(response.getShipWard()).isEqualTo("Ben Nghe");
        assertThat(response.getShipDistrict()).isEqualTo("District 1");
        assertThat(response.getShipCity()).isEqualTo("Ho Chi Minh");
        assertThat(response.getShipPostalCode()).isEqualTo("700000");
    }

    @Test
    void makePaymentDebitsWalletWithPurchaseTransactionReferencingTheOrder() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        Wallet wallet = wallet(customer, new BigDecimal("100000000"));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(walletService.getOrCreate(customer)).thenReturn(wallet);

        orderService.makePayment(customer, 50L);

        verify(walletService).debit(eq(wallet), amountCaptor.capture(), eq(CoinTxType.PURCHASE),
                eq("ORDER"), eq(50L), isNull());
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(new BigDecimal("30030000"));
    }

    @Test
    void makePaymentSavesSuccessfulCoinPayment() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(walletService.getOrCreate(customer)).thenReturn(wallet(customer, new BigDecimal("100000000")));

        OrderResponse response = orderService.makePayment(customer, 50L);

        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getOrder()).isSameAs(order);
        assertThat(saved.getMethod()).isEqualTo(PaymentMethod.COIN);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("30030000"));
        assertThat(saved.getTransactionId()).isNotBlank();
        assertThat(saved.getPaidAt()).isNotNull();

        assertThat(order.getPayments()).containsExactly(saved);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.COIN);
        assertThat(response.getTransactionId()).isEqualTo(saved.getTransactionId());
    }

    @Test
    void makePaymentMarksOrderAsPaid() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(walletService.getOrCreate(customer)).thenReturn(wallet(customer, new BigDecimal("100000000")));

        OrderResponse response = orderService.makePayment(customer, 50L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void makePaymentThrowsWhenOrderIsNotFound() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.makePayment(customer, 999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order not found");

        verifyNoInteractions(walletService);
    }

    @Test
    void makePaymentThrowsWhenOrderBelongsToAnotherUser() {
        User owner = user(1L, "nguyen.van.an@gmail.com");
        User intruder = user(2L, "tran.thi.b@gmail.com");
        Order order = orderWith(50L, owner, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.makePayment(intruder, 50L))
                .isInstanceOf(ResourceAccessDeniedException.class)
                .hasMessage("You do not have access to this order");

        verifyNoInteractions(walletService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void makePaymentThrowsAndDoesNotDebitWhenOrderIsNotPending() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.SHIPPED,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.makePayment(customer, 50L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order is not in payable state");

        verify(walletService, never()).debit(any(), any(), any(), anyString(), anyLong(), any());
        verifyNoInteractions(paymentRepository);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void makePaymentThrowsAndDoesNotDebitWhenOrderAlreadyHasSuccessfulPayment() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        order.getPayments().add(payment(order, PaymentStatus.SUCCESS));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.makePayment(customer, 50L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order has already been paid");

        verify(walletService, never()).debit(any(), any(), any(), anyString(), anyLong(), any());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void makePaymentAllowsRetryWhenPreviousPaymentFailed() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        order.getPayments().add(payment(order, PaymentStatus.FAILED));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(walletService.getOrCreate(customer)).thenReturn(wallet(customer, new BigDecimal("100000000")));

        orderService.makePayment(customer, 50L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void makePaymentLeavesOrderPendingWhenBalanceIsInsufficient() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 10, 1));
        Wallet wallet = wallet(customer, BigDecimal.ZERO);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(walletService.getOrCreate(customer)).thenReturn(wallet);
        when(walletService.debit(eq(wallet), any(BigDecimal.class), eq(CoinTxType.PURCHASE),
                eq("ORDER"), eq(50L), isNull()))
                .thenThrow(new InsufficientBalanceException("Insufficient coin balance"));

        assertThatThrownBy(() -> orderService.makePayment(customer, 50L))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient coin balance");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPayments()).isEmpty();
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void cancelOrderSetsStatusToCancelled() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.cancelOrder(customer, 50L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrderRestoresInventoryForEachItem() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2),
                new ItemSpec("AirPods Pro", new BigDecimal("5000000"), 4, 3));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        Inventory first = order.getItems().get(0).getProduct().getInventory();
        Inventory second = order.getItems().get(1).getProduct().getInventory();

        orderService.cancelOrder(customer, 50L);

        assertThat(first.getQuantity()).isEqualTo(10);
        assertThat(second.getQuantity()).isEqualTo(7);
    }

    @Test
    void cancelOrderThrowsWhenOrderIsNotFound() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(customer, 999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order not found");
    }

    @Test
    void cancelOrderThrowsWhenOrderBelongsToAnotherUser() {
        User owner = user(1L, "nguyen.van.an@gmail.com");
        User intruder = user(2L, "tran.thi.b@gmail.com");
        Order order = orderWith(50L, owner, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        Inventory inventory = order.getItems().get(0).getProduct().getInventory();

        assertThatThrownBy(() -> orderService.cancelOrder(intruder, 50L))
                .isInstanceOf(ResourceAccessDeniedException.class)
                .hasMessage("You do not have access to this order");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(inventory.getQuantity()).isEqualTo(8);
    }

    @Test
    void cancelOrderThrowsWhenOrderIsAlreadyPaid() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PAID,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        Inventory inventory = order.getItems().get(0).getProduct().getInventory();

        assertThatThrownBy(() -> orderService.cancelOrder(customer, 50L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only pending orders can be cancelled");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(inventory.getQuantity()).isEqualTo(8);
    }

    @Test
    void cancelOrderThrowsWhenOrderIsAlreadyCancelled() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.CANCELLED,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        Inventory inventory = order.getItems().get(0).getProduct().getInventory();

        assertThatThrownBy(() -> orderService.cancelOrder(customer, 50L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only pending orders can be cancelled");

        assertThat(inventory.getQuantity()).isEqualTo(8);
    }

    @Test
    void updateOrderStatusMovesPaidOrderToShipped() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PAID,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.updateOrderStatus(50L, updateStatusRequest(OrderStatus.SHIPPED));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void updateOrderStatusThrowsWhenOrderIsNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(999L, updateStatusRequest(OrderStatus.SHIPPED)))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order not found");
    }

    @Test
    void updateOrderStatusRefusesToUpdateDeliveredOrder() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.DELIVERED,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(50L, updateStatusRequest(OrderStatus.SHIPPED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Delivered orders cannot be updated");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void updateOrderStatusRefusesToUpdateCancelledOrder() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.CANCELLED,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(50L, updateStatusRequest(OrderStatus.SHIPPED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cancelled orders cannot be updated");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void updateOrderStatusRefusesCancelledAsTargetStatusSoRestockAndRefundAreNotSkipped() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PAID,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        Inventory inventory = order.getItems().get(0).getProduct().getInventory();

        assertThatThrownBy(() -> orderService.updateOrderStatus(50L, updateStatusRequest(OrderStatus.CANCELLED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Orders cannot be cancelled through a status update");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(inventory.getQuantity()).isEqualTo(8);
        verifyNoInteractions(walletService);
    }

    @Test
    void viewOrderHistoryMapsEachOrderToASummary() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PAID,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2),
                new ItemSpec("AirPods Pro", new BigDecimal("5000000"), 8, 1));
        order.getPayments().add(payment(order, PaymentStatus.SUCCESS));
        when(orderRepository.findByUserOrderByCreatedAtDesc(customer)).thenReturn(List.of(order));

        List<OrderSummaryResponse> history = orderService.viewOrderHistory(customer);

        assertThat(history).hasSize(1);
        OrderSummaryResponse summary = history.get(0);
        assertThat(summary.getOrderId()).isEqualTo(50L);
        assertThat(summary.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(summary.getTotalAmount()).isEqualByComparingTo(new BigDecimal("65030000"));
        assertThat(summary.getItemCount()).isEqualTo(2);
        assertThat(summary.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void viewOrderHistoryFallsBackToPendingPaymentStatusWhenOrderHasNoPayments() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 1));
        when(orderRepository.findByUserOrderByCreatedAtDesc(customer)).thenReturn(List.of(order));

        List<OrderSummaryResponse> history = orderService.viewOrderHistory(customer);

        assertThat(history.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void viewOrderHistoryReturnsEmptyListWhenUserHasNoOrders() {
        User customer = user(1L, "nguyen.van.an@gmail.com");
        when(orderRepository.findByUserOrderByCreatedAtDesc(customer)).thenReturn(List.of());

        assertThat(orderService.viewOrderHistory(customer)).isEmpty();
    }

    @Test
    void getAllOrdersAppliesBothFiltersWhenEmailAndStatusAreProvided() {
        Pageable pageable = PageRequest.of(0, 12);
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PAID,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 1));
        when(orderRepository.findByUserEmailContainingIgnoreCaseAndStatus("nguyen", OrderStatus.PAID, pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<AdminOrderSummaryResponse> page = orderService.getAllOrders("nguyen", OrderStatus.PAID, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserEmail()).isEqualTo("nguyen.van.an@gmail.com");
        verify(orderRepository, never()).findByUserEmailContainingIgnoreCase(any(String.class), any(Pageable.class));
        verify(orderRepository, never()).findByStatus(any(OrderStatus.class), any(Pageable.class));
        verify(orderRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllOrdersFiltersByEmailAloneWhenStatusIsNull() {
        Pageable pageable = PageRequest.of(0, 12);
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 1));
        when(orderRepository.findByUserEmailContainingIgnoreCase("nguyen", pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<AdminOrderSummaryResponse> page = orderService.getAllOrders("nguyen", null, pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(orderRepository, never()).findByStatus(any(OrderStatus.class), any(Pageable.class));
        verify(orderRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllOrdersFiltersByStatusWhenEmailIsBlank() {
        Pageable pageable = PageRequest.of(0, 12);
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PAID,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 1));
        when(orderRepository.findByStatus(OrderStatus.PAID, pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<AdminOrderSummaryResponse> page = orderService.getAllOrders("  ", OrderStatus.PAID, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllOrdersReturnsEveryOrderWhenNoFilterIsProvided() {
        Pageable pageable = PageRequest.of(0, 12);
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 1));
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<AdminOrderSummaryResponse> page = orderService.getAllOrders(null, null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(orderRepository, never()).findByStatus(any(OrderStatus.class), any(Pageable.class));
    }

    @Test
    void getAllOrdersMapsOrderFieldsAndPaymentStatusOfFirstPayment() {
        Pageable pageable = PageRequest.of(0, 12);
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PAID,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 2),
                new ItemSpec("AirPods Pro", new BigDecimal("5000000"), 8, 1));
        order.getPayments().add(payment(order, PaymentStatus.SUCCESS));
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        AdminOrderSummaryResponse summary = orderService.getAllOrders(null, null, pageable).getContent().get(0);

        assertThat(summary.getOrderId()).isEqualTo(50L);
        assertThat(summary.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(summary.getTotalAmount()).isEqualByComparingTo(new BigDecimal("65030000"));
        assertThat(summary.getItemCount()).isEqualTo(2);
        assertThat(summary.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(summary.getUserEmail()).isEqualTo("nguyen.van.an@gmail.com");
    }

    @Test
    void getAllOrdersFallsBackToPendingPaymentStatusWhenOrderHasNoPayments() {
        Pageable pageable = PageRequest.of(0, 12);
        User customer = user(1L, "nguyen.van.an@gmail.com");
        Order order = orderWith(50L, customer, OrderStatus.PENDING,
                new ItemSpec("iPhone 15 Pro", new BigDecimal("30000000"), 8, 1));
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        AdminOrderSummaryResponse summary = orderService.getAllOrders(null, null, pageable).getContent().get(0);

        assertThat(summary.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}
