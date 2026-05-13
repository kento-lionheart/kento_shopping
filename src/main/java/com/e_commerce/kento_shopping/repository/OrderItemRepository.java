package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.Order;
import com.e_commerce.kento_shopping.entity.OrderItem;
import com.e_commerce.kento_shopping.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
    boolean existsByProduct(Product product);
}
