package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.CartItemRequest;
import com.e_commerce.kento_shopping.dto.request.UpdateCartItemQuantityRequest;
import com.e_commerce.kento_shopping.dto.response.CartResponse;
import com.e_commerce.kento_shopping.entity.User;

public interface CartService {
    CartResponse getCart(User user);
    CartResponse addItem(User user, CartItemRequest request);
    CartResponse updateItem(User user, Long productId, UpdateCartItemQuantityRequest request);
    CartResponse removeItem(User user, Long productId);
    CartResponse clearCart(User user);
}
