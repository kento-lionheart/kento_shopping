package com.e_commerce.kento_shopping.entity;

import com.e_commerce.kento_shopping.enums.CoinTxType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "COIN_TRANSACTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinTransaction extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private CoinTxType type;

    // Signed: TOP_UP and REFUND positive, PURCHASE negative, ADJUSTMENT either.
    @Column(nullable = false)
    @NotNull
    private BigDecimal amount;

    // Snapshot, for the same reason OrderItem keeps priceAtPurchase.
    @Column(nullable = false)
    @NotNull
    private BigDecimal balanceAfter;

    // What caused this row: "ORDER" or "TOP_UP_REQUEST", and its id.
    private String referenceType;

    private Long referenceId;

    private String note;
}
