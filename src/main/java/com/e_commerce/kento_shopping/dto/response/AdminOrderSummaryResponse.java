package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.enums.OrderStatus;
import com.e_commerce.kento_shopping.enums.PaymentStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class AdminOrderSummaryResponse extends OrderSummaryResponse{
    private String userEmail;
    public AdminOrderSummaryResponse(Long orderId, OrderStatus status, BigDecimal totalAmount,
                                     int itemCount, PaymentStatus paymentStatus,
                                     LocalDateTime createdAt, String userEmail) {
        super(orderId, status, totalAmount, itemCount, paymentStatus, createdAt);
        this.userEmail = userEmail;
    }
}
