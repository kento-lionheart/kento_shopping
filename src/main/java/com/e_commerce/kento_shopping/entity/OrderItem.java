package com.e_commerce.kento_shopping.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ORDER_ITEM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity{
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull @Column(nullable = false)
    private String productName;

    @NotNull @Column(nullable = false)
    @Min(1)
    private Integer quantity;

    @NotNull @Column(nullable = false)
    private BigDecimal priceAtPurchase;
}
