package com.e_commerce.kento_shopping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class WalletResponse {
    private BigDecimal balance;
    private List<CoinTransactionResponse> recentTransactions;
}
