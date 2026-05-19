package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.CheckoutRequest;
import com.e_commerce.kento_shopping.dto.request.PaymentRequest;
import com.e_commerce.kento_shopping.dto.response.OrderResponse;
import com.e_commerce.kento_shopping.dto.response.OrderSummaryResponse;
import com.e_commerce.kento_shopping.entity.User;

import java.util.List;

public interface OrderService {
    OrderResponse checkout(User user, CheckoutRequest request);
    OrderResponse makePayment(User user, Long orderId, PaymentRequest request);
    List<OrderSummaryResponse> viewOrderHistory(User user);
    OrderResponse cancelOrder(User user, Long orderId);
}
