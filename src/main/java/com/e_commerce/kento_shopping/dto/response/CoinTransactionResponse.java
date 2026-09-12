package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.enums.CoinTxType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CoinTransactionResponse {
    private Long id;
    private CoinTxType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String referenceType;
    private Long referenceId;
    private String note;
    private LocalDateTime createdAt;
}
