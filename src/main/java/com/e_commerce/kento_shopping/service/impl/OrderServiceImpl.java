package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.CheckoutRequest;
import com.e_commerce.kento_shopping.dto.response.OrderItemResponse;
import com.e_commerce.kento_shopping.dto.response.OrderResponse;
import com.e_commerce.kento_shopping.entity.*;
import com.e_commerce.kento_shopping.enums.OrderStatus;
import com.e_commerce.kento_shopping.enums.PaymentMethod;
import com.e_commerce.kento_shopping.enums.PaymentStatus;
import com.e_commerce.kento_shopping.exception.CartNotFoundException;
import com.e_commerce.kento_shopping.exception.InsufficientStockException;
import com.e_commerce.kento_shopping.repository.CartRepository;
import com.e_commerce.kento_shopping.repository.OrderRepository;
import com.e_commerce.kento_shopping.repository.PaymentRepository;
import com.e_commerce.kento_shopping.service.OrderService;
import lombok.RequiredArgsConstructor;
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
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
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

        Payment payment = order.getPayments().get(0);

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
                payment.getStatus(),
                payment.getMethod(),
                payment.getTransactionId(),
                items,
                order.getCreatedAt()
        );
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
        // calculate total fee
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
        //mock the paying method
        Payment payment = Payment.builder()
                .order(order)
                .method(request.getPaymentMethod())
                .amount(totalAmount)
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

        cart.getItems().clear();
        return mapToOrderResponse(order);
    }
}
