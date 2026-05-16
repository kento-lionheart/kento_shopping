package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.CartItemRequest;
import com.e_commerce.kento_shopping.dto.request.UpdateCartItemQuantityRequest;
import com.e_commerce.kento_shopping.dto.response.CartItemResponse;
import com.e_commerce.kento_shopping.dto.response.CartResponse;
import com.e_commerce.kento_shopping.entity.*;
import com.e_commerce.kento_shopping.exception.CartItemNotFoundException;
import com.e_commerce.kento_shopping.exception.CartNotFoundException;
import com.e_commerce.kento_shopping.exception.InsufficientStockException;
import com.e_commerce.kento_shopping.exception.ProductNotFoundException;
import com.e_commerce.kento_shopping.repository.CartRepository;
import com.e_commerce.kento_shopping.repository.ProductRepository;
import com.e_commerce.kento_shopping.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    private CartResponse mapToResponse(Cart cart){
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    BigDecimal price = item.getProduct().getPrice();
                    int quantity = item.getQuantity();
                    BigDecimal subTotal = price.multiply(BigDecimal.valueOf(quantity));
                    Inventory inventory = item.getProduct().getInventory();
                    String stockStatus = inventory != null && inventory.getQuantity() > 0
                            ? "IN_STOCK" : "OUT_OF_STOCK";
                    return new CartItemResponse(
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            price,
                            subTotal,
                            item.getProduct().getImageUrl(),
                            quantity,
                            stockStatus
                    );
                })
                .toList();
        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int itemCount = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();
        return new CartResponse(items, total, itemCount);
    }
    @Override
    public CartResponse getCart(User user) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(User user, CartItemRequest request) {
        Cart userCart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
        userCart.getItems().stream()
                .filter(cartItem -> cartItem.getProduct().getId().equals(request.getProductId()))
                .findAny()
                .ifPresentOrElse(cartItem -> {
                    cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
                },
                () ->{
                    Product product = productRepository.findById(request.getProductId())
                            .orElseThrow(() -> new ProductNotFoundException("Product not found"));
                    Inventory inventory = product.getInventory();
                    if(inventory == null || inventory.getQuantity() < request.getQuantity()){
                        throw new InsufficientStockException("Insufficient stock");
                    }
                    CartItem newItem = CartItem.builder()
                            .id(new CartItem.Id(userCart.getId(), product.getId()))
                            .cart(userCart)
                            .product(product)
                            .quantity(request.getQuantity())
                            .build();
                    userCart.getItems().add(newItem);
                });
        return mapToResponse(userCart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(User user, Long productId, UpdateCartItemQuantityRequest request) {
        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        CartItem cartItem = userCart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findAny()
                .orElseThrow(() -> new CartItemNotFoundException("Item not in cart"));
        Inventory inventory = cartItem.getProduct().getInventory();
        if (inventory == null || inventory.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock");
        }
        cartItem.setQuantity(request.getQuantity());
        return mapToResponse(userCart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(User user, Long productId) {
        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        CartItem cartItem = userCart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findAny()
                .orElseThrow(() -> new CartItemNotFoundException("Item not in cart"));
        userCart.getItems().remove(cartItem);
        return mapToResponse(userCart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(User user) {
        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        userCart.getItems().clear();
        return mapToResponse(userCart);
    }
}
