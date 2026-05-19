package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.CheckoutRequest;
import com.e_commerce.kento_shopping.dto.request.PaymentRequest;
import com.e_commerce.kento_shopping.dto.request.admin.UpdateOrderStatusRequest;
import com.e_commerce.kento_shopping.dto.response.AdminOrderSummaryResponse;
import com.e_commerce.kento_shopping.dto.response.OrderResponse;
import com.e_commerce.kento_shopping.dto.response.OrderSummaryResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse checkout(User user, CheckoutRequest request);
    OrderResponse makePayment(User user, Long orderId, PaymentRequest request);
    List<OrderSummaryResponse> viewOrderHistory(User user);
    OrderResponse cancelOrder(User user, Long orderId);
    Page<AdminOrderSummaryResponse> getAllOrders(String email, OrderStatus status, Pageable pageable);
    OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request);
}
