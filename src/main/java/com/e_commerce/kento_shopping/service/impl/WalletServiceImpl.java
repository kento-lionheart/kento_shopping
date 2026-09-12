package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.response.CoinTransactionResponse;
import com.e_commerce.kento_shopping.dto.response.WalletResponse;
import com.e_commerce.kento_shopping.entity.CoinTransaction;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.entity.Wallet;
import com.e_commerce.kento_shopping.enums.CoinTxType;
import com.e_commerce.kento_shopping.exception.InsufficientBalanceException;
import com.e_commerce.kento_shopping.exception.WalletNotFoundException;
import com.e_commerce.kento_shopping.repository.CoinTransactionRepository;
import com.e_commerce.kento_shopping.repository.UserRepository;
import com.e_commerce.kento_shopping.repository.WalletRepository;
import com.e_commerce.kento_shopping.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final DecimalFormat COINS = new DecimalFormat("#,###");

    private final WalletRepository walletRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final UserRepository userRepository;

    private CoinTransactionResponse mapToTransactionResponse(CoinTransaction tx) {
        return new CoinTransactionResponse(
                tx.getId(), tx.getType(), tx.getAmount(), tx.getBalanceAfter(),
                tx.getReferenceType(), tx.getReferenceId(), tx.getNote(), tx.getCreatedAt());
    }

    private WalletResponse mapToWalletResponse(Wallet wallet) {
        List<CoinTransactionResponse> recent =
                coinTransactionRepository.findTop10ByWalletOrderByCreatedAtDesc(wallet).stream()
                        .map(this::mapToTransactionResponse)
                        .toList();
        return new WalletResponse(wallet.getBalance(), recent);
    }

    @Override
    @Transactional
    public Wallet getOrCreate(User user) {
        return walletRepository.findByUser(user).orElseGet(() -> {
            try {
                return walletRepository.saveAndFlush(Wallet.builder()
                        .user(user).balance(BigDecimal.ZERO).build());
            } catch (DataIntegrityViolationException e) {
                // Lost the race against a concurrent first access; the unique
                // constraint on user_id means the winner's row is now readable.
                return walletRepository.findByUser(user)
                        .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
            }
        });
    }

    @Override
    @Transactional
    public WalletResponse getWallet(User user) {
        return mapToWalletResponse(getOrCreate(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CoinTransactionResponse> getTransactions(User user, Pageable pageable) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
        return coinTransactionRepository.findByWalletOrderByCreatedAtDesc(wallet, pageable)
                .map(this::mapToTransactionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletOf(Long userId) {
        return mapToWalletResponse(walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "This user has no wallet — staff and admin accounts cannot hold one")));
    }

    @Override
    @Transactional
    public CoinTransaction credit(Wallet wallet, BigDecimal amount, CoinTxType type,
                                  String referenceType, Long referenceId, String note) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        return write(wallet, amount, type, referenceType, referenceId, note);
    }

    @Override
    @Transactional
    public CoinTransaction debit(Wallet wallet, BigDecimal amount, CoinTxType type,
                                 String referenceType, Long referenceId, String note) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(String.format(
                    "Insufficient coin balance. Required: %s — available: %s",
                    COINS.format(amount), COINS.format(wallet.getBalance())));
        }
        return write(wallet, amount.negate(), type, referenceType, referenceId, note);
    }

    /**
     * The only place the balance moves. Balance and ledger row are written in
     * the same transaction, which is what keeps SUM(ledger) == balance true.
     */
    private CoinTransaction write(Wallet wallet, BigDecimal signedAmount, CoinTxType type,
                                  String referenceType, Long referenceId, String note) {
        BigDecimal newBalance = wallet.getBalance().add(signedAmount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        return coinTransactionRepository.save(CoinTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(signedAmount)
                .balanceAfter(newBalance)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .note(note)
                .build());
    }
}
