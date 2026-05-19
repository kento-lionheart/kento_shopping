package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.Order;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByUserEmailContainingIgnoreCase(String email, Pageable pageable);
}
