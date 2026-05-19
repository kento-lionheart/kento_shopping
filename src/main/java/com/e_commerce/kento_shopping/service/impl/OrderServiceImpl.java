package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.CheckoutRequest;
import com.e_commerce.kento_shopping.dto.request.PaymentRequest;
import com.e_commerce.kento_shopping.dto.request.admin.UpdateOrderStatusRequest;
import com.e_commerce.kento_shopping.dto.response.AdminOrderSummaryResponse;
import com.e_commerce.kento_shopping.dto.response.OrderItemResponse;
import com.e_commerce.kento_shopping.dto.response.OrderResponse;
import com.e_commerce.kento_shopping.dto.response.OrderSummaryResponse;
import com.e_commerce.kento_shopping.entity.*;
import com.e_commerce.kento_shopping.enums.OrderStatus;
import com.e_commerce.kento_shopping.enums.PaymentMethod;
import com.e_commerce.kento_shopping.enums.PaymentStatus;
import com.e_commerce.kento_shopping.exception.CartNotFoundException;
import com.e_commerce.kento_shopping.exception.InsufficientStockException;
import com.e_commerce.kento_shopping.exception.OrderNotFoundException;
import com.e_commerce.kento_shopping.repository.CartRepository;
import com.e_commerce.kento_shopping.repository.OrderRepository;
import com.e_commerce.kento_shopping.repository.PaymentRepository;
import com.e_commerce.kento_shopping.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem){
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getProduct().getImageUrl(),
                orderItem.getPriceAtPurchase(),
                orderItem.getPriceAtPurchase().multiply(BigDecimal.valueOf(orderItem.getQuantity()))
        );
    }
    private OrderResponse mapToOrderResponse(Order order){
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .toList();
        Payment payment = order.getPayments().isEmpty() ? null : order.getPayments().get(0);
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getShipRecipientName(),
                order.getShipPhone(),
                order.getShipStreet(),
                order.getShipWard(),
                order.getShipDistrict(),
                order.getShipCity(),
                order.getShipPostalCode(),
                payment != null ? payment.getStatus() : null,
                payment != null ? payment.getMethod() : null,
                payment != null ? payment.getTransactionId() : null,
                items,
                order.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        if(!order.getUser().getId().equals(user.getId())){
            throw new IllegalArgumentException("You don't have access to this order !!!");
        }
        if(order.getStatus()!= OrderStatus.PENDING){
            throw new IllegalArgumentException("Only pending orders can be cancelled");
        }
        for(OrderItem item : order.getItems()){
            Inventory inv = item.getProduct().getInventory();
            inv.setQuantity(inv.getQuantity() + item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderSummaryResponse> getAllOrders(String email, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if(email != null && !email.isBlank()){
            orders = orderRepository.findByUserEmailContainingIgnoreCase(email, pageable);
        }
        else if(status != null){
            orders = orderRepository.findByStatus(status,pageable);
        }
        else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(order -> {
            PaymentStatus paymentStatus = order.getPayments().isEmpty()
                    ? PaymentStatus.PENDING
                    : order.getPayments().get(0).getStatus();
            return new AdminOrderSummaryResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getItems().size(),
                    paymentStatus,
                    order.getCreatedAt(),
                    order.getUser().getEmail()
            );
        });
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Delivered orders cannot be updated");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled orders cannot be updated");
        }
        order.setStatus(request.getStatus());
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse checkout(User user, CheckoutRequest request) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        if(cart.getItems().isEmpty()){
            throw new IllegalArgumentException("Cart is empty");
        }
        //Validate stock
        cart.getItems().stream()
                .filter(cartitem ->{
                    Inventory inv = cartitem.getProduct().getInventory();
                    return inv == null || inv.getQuantity() < cartitem.getQuantity();
                })
                .findFirst()
                .ifPresent(cartItem ->{
                    throw new InsufficientStockException(
                            cartItem.getProduct().getName() + " has insufficient stock"
                    );
                });
        // calculate total fee -> shippingFee is concrete 30k vndong baby
        BigDecimal subTotal = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = BigDecimal.valueOf(30000);
        BigDecimal totalAmount = subTotal.add(shippingFee);
        // Create an order
        Order order = Order.builder()
                .user(user)
                .subtotal(subTotal)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .shippingFee(shippingFee)
                .shipRecipientName(request.getRecipientName())
                .shipPhone(request.getPhone())
                .shipStreet(request.getStreet())
                .shipWard(request.getWard())
                .shipDistrict(request.getDistrict())
                .shipCity(request.getCity())
                .shipPostalCode(request.getPostalCode())
                .build();
        orderRepository.save(order);
        //Save orderItems, sub the quantity
        for(CartItem cartItem : cart.getItems()){
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .productName(cartItem.getProduct().getName())
                    .priceAtPurchase(cartItem.getProduct().getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();
            order.getItems().add(orderItem);
            Inventory inv = cartItem.getProduct().getInventory();
            inv.setQuantity(inv.getQuantity() - cartItem.getQuantity());
        }

        cart.getItems().clear();
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> viewOrderHistory(User user) {
        List<Order> orderResponse = orderRepository.findByUserOrderByCreatedAtDesc(user);

        return orderResponse.stream()
                .map(order ->{
                    PaymentStatus paymentStatus;
                    if(order.getPayments().isEmpty()){
                        paymentStatus = PaymentStatus.PENDING;
                    }
                    else {
                      paymentStatus = order.getPayments().get(0).getStatus();
                    }
                    return new OrderSummaryResponse(
                            order.getId(),
                            order.getStatus(),
                            order.getTotalAmount(),
                            order.getItems().size(),
                            paymentStatus,
                            order.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse makePayment(User user, Long orderId, PaymentRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        if(!order.getUser().getId().equals(user.getId())){
            throw new IllegalArgumentException("You do not have access to this order");
        }
        if(order.getStatus() != OrderStatus.PENDING){
            throw new IllegalArgumentException("Order is not in payable state");
        }
        boolean alreadyPaid = order.getPayments().stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.SUCCESS);
        if(alreadyPaid){
            throw new IllegalArgumentException("Order has already been paid");
        }
        Payment payment = Payment.builder()
                .order(order)
                .method(request.getPaymentMethod())
                .amount(order.getTotalAmount())
                .transactionId(UUID.randomUUID().toString())
                .build();
        if (request.getPaymentMethod() == PaymentMethod.MOMO) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            order.setStatus(OrderStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.PENDING);
        }

        order.getPayments().add(payment);
        paymentRepository.save(payment);

        return mapToOrderResponse(order);
    }
}
