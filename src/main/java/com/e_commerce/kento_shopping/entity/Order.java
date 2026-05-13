package com.e_commerce.kento_shopping.entity;

import com.e_commerce.kento_shopping.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

@Entity
@Table(name= "ORDERS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity{

    @Column(nullable = false)
    @NotNull
    private BigDecimal subtotal;

    @Column(nullable = false)
    @NotNull
    private BigDecimal shippingFee;

    @Column(nullable = false)
    @NotNull
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private OrderStatus status;

    @Column(nullable = false)
    @NotNull
    private String shipRecipientName;

    @Column(nullable = false)
    @NotNull
    private String shipPhone;

    @Column(nullable = false)
    @NotNull
    private String shipStreet;

    @Column(nullable = false)
    @NotNull
    private String shipWard;

    @Column(nullable = false)
    @NotNull
    private String shipDistrict;

    @Column(nullable = false)
    @NotNull
    private String shipCity;

    private String shipPostalCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();
}
