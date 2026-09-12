package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.CoinTransaction;
import com.e_commerce.kento_shopping.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {
    Page<CoinTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet, Pageable pageable);
    List<CoinTransaction> findTop10ByWalletOrderByCreatedAtDesc(Wallet wallet);
}
