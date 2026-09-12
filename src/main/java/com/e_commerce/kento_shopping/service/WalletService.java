package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.response.CoinTransactionResponse;
import com.e_commerce.kento_shopping.dto.response.WalletResponse;
import com.e_commerce.kento_shopping.entity.CoinTransaction;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.entity.Wallet;
import com.e_commerce.kento_shopping.enums.CoinTxType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface WalletService {

    Wallet getOrCreate(User user);

    WalletResponse getWallet(User user);

    Page<CoinTransactionResponse> getTransactions(User user, Pageable pageable);

    WalletResponse getWalletOf(Long userId);

    CoinTransaction credit(Wallet wallet, BigDecimal amount, CoinTxType type,
                           String referenceType, Long referenceId, String note);

    CoinTransaction debit(Wallet wallet, BigDecimal amount, CoinTxType type,
                          String referenceType, Long referenceId, String note);
}
