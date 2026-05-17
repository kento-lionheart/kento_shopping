package com.e_commerce.kento_shopping.controller;

import com.e_commerce.kento_shopping.dto.request.CheckoutRequest;
import com.e_commerce.kento_shopping.dto.response.OrderResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal User user,
                                                 @Valid @RequestBody CheckoutRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(user, request));
    }
}
