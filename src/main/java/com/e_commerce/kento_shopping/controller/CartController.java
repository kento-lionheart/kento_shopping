package com.e_commerce.kento_shopping.controller;

import com.e_commerce.kento_shopping.dto.request.CartItemRequest;
import com.e_commerce.kento_shopping.dto.request.UpdateCartItemQuantityRequest;
import com.e_commerce.kento_shopping.dto.response.CartResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(cartService.getCart(user));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CartItemRequest request){
        return ResponseEntity.ok(cartService.addItem(user, request));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request){
        return ResponseEntity.ok(cartService.updateItem(user, productId, request));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId){
        return ResponseEntity.ok(cartService.removeItem(user, productId));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(cartService.clearCart(user));
    }
}
