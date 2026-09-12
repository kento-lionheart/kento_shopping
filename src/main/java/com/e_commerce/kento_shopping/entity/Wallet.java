package com.e_commerce.kento_shopping.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "WALLET", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    // Materialised total. The COIN_TRANSACTION ledger is the source of truth:
    // SUM(ledger.amount) must always equal this.
    @Column(nullable = false)
    @NotNull
    @PositiveOrZero
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private Integer version;
}
