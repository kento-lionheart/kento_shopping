package com.e_commerce.kento_shopping.controller;

import com.e_commerce.kento_shopping.dto.request.CheckoutRequest;
import com.e_commerce.kento_shopping.dto.request.PaymentRequest;
import com.e_commerce.kento_shopping.dto.response.OrderResponse;
import com.e_commerce.kento_shopping.dto.response.OrderSummaryResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> viewOrderHistory(
            @AuthenticationPrincipal User user){
        return ResponseEntity.ok(orderService.viewOrderHistory(user));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal User user,
                                                 @Valid @RequestBody CheckoutRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(user, request));
    }

    @PostMapping("/{orderId}/payment")
    public ResponseEntity<OrderResponse> makePayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.makePayment(user, orderId, request));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId){
        return ResponseEntity.ok(orderService.cancelOrder(user, orderId));
    }
}
