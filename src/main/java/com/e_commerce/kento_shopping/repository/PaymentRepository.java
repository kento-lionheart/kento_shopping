package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.Order;
import com.e_commerce.kento_shopping.entity.Payment;
import com.e_commerce.kento_shopping.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByOrderOrderByCreatedAtDesc(Order order);
    Optional<Payment> findByOrderAndStatus(Order order, PaymentStatus paymentStatus);
}
