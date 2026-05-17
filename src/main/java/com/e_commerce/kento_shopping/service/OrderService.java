package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.CheckoutRequest;
import com.e_commerce.kento_shopping.dto.response.OrderResponse;
import com.e_commerce.kento_shopping.entity.User;

public interface OrderService {
    OrderResponse checkout(User user, CheckoutRequest request);
}
