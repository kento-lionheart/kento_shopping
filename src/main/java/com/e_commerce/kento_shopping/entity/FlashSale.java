package com.e_commerce.kento_shopping.entity;

import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "FLASH_SALE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSale extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull
    private Product product;

    @Column(nullable = false)
    @NotNull
    private String name;

    @Column(nullable = false)
    @NotNull
    @Positive
    private BigDecimal salePrice;

    @Column(nullable = false)
    @NotNull
    @Positive
    private Integer allocatedQty;

    @Column(nullable = false)
    @NotNull
    @PositiveOrZero
    @Builder.Default
    private Integer soldQty = 0;

    @Column(nullable = false)
    @NotNull
    private LocalDateTime startAt;

    @Column(nullable = false)
    @NotNull
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    @Builder.Default
    private FlashSaleStatus status = FlashSaleStatus.SCHEDULED;

    @Version
    private Integer version;
}
