package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.enums.OrderStatus;
import com.e_commerce.kento_shopping.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderSummaryResponse {
    private Long orderId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
}
