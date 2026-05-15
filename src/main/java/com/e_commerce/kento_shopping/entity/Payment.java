package com.e_commerce.kento_shopping.entity;

import com.e_commerce.kento_shopping.enums.PaymentMethod;
import com.e_commerce.kento_shopping.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity{
    @Column(nullable = false, unique = true)
    @NotNull
    private String transactionId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private PaymentMethod method;

    @Column(nullable = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @NotNull
    @Column(nullable = false)
    @Positive
    private BigDecimal amount;

    private LocalDateTime paidAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
}
