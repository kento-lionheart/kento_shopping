package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.Cart;
import com.e_commerce.kento_shopping.entity.CartItem;
import com.e_commerce.kento_shopping.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, CartItem.Id> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    void deleteByCart(Cart cart);
}
