package com.e_commerce.kento_shopping.service;

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
import com.e_commerce.kento_shopping.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CoinTransactionRepository coinTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Captor
    private ArgumentCaptor<CoinTransaction> txCaptor;

    @Captor
    private ArgumentCaptor<Wallet> walletCaptor;

    private User customer;

    @BeforeEach
    void setUp() {
        customer = user(1L, "nguyen.van.an@gmail.com");
    }

    private User user(Long id, String email) {
        User u = User.builder()
                .email(email)
                .password("encoded")
                .fullName("Nguyen Van An")
                .phoneNumber("0900000000")
                .build();
        u.setId(id);
        return u;
    }

    private Wallet wallet(Long id, User owner, String balance) {
        Wallet w = Wallet.builder()
                .user(owner)
                .balance(new BigDecimal(balance))
                .build();
        w.setId(id);
        return w;
    }

    private CoinTransaction transaction(Long id, Wallet w, CoinTxType type, String amount, String balanceAfter) {
        CoinTransaction tx = CoinTransaction.builder()
                .wallet(w)
                .type(type)
                .amount(new BigDecimal(amount))
                .balanceAfter(new BigDecimal(balanceAfter))
                .referenceType("TOP_UP_REQUEST")
                .referenceId(7L)
                .note("seed")
                .build();
        tx.setId(id);
        return tx;
    }

    private void echoSavedTransaction() {
        when(coinTransactionRepository.save(any(CoinTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getOrCreateReturnsExistingWallet() {
        Wallet existing = wallet(10L, customer, "5000");
        when(walletRepository.findByUser(customer)).thenReturn(Optional.of(existing));

        Wallet result = walletService.getOrCreate(customer);

        assertThat(result).isSameAs(existing);
        verify(walletRepository, never()).saveAndFlush(any(Wallet.class));
    }

    @Test
    void getOrCreateCreatesZeroBalanceWalletWhenNoneExists() {
        when(walletRepository.findByUser(customer)).thenReturn(Optional.empty());
        when(walletRepository.saveAndFlush(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.getOrCreate(customer);

        verify(walletRepository).saveAndFlush(walletCaptor.capture());
        assertThat(walletCaptor.getValue().getUser()).isSameAs(customer);
        assertThat(walletCaptor.getValue().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getOrCreateRereadsWalletWhenConcurrentInsertLosesTheRace() {
        Wallet winner = wallet(11L, customer, "0");
        when(walletRepository.findByUser(customer))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(walletRepository.saveAndFlush(any(Wallet.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate user_id"));

        Wallet result = walletService.getOrCreate(customer);

        assertThat(result).isSameAs(winner);
        verify(walletRepository, times(2)).findByUser(customer);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void getOrCreateThrowsWalletNotFoundWhenRereadAfterLostRaceIsAlsoEmpty() {
        when(walletRepository.findByUser(customer)).thenReturn(Optional.empty());
        when(walletRepository.saveAndFlush(any(Wallet.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate user_id"));

        assertThatThrownBy(() -> walletService.getOrCreate(customer))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("Wallet not found");
    }

    @Test
    void creditIncreasesBalanceAndWritesPositiveLedgerRow() {
        Wallet w = wallet(10L, customer, "5000");
        echoSavedTransaction();

        CoinTransaction tx = walletService.credit(w, new BigDecimal("2500"), CoinTxType.TOP_UP,
                "TOP_UP_REQUEST", 42L, "approved by admin");

        assertThat(w.getBalance()).isEqualByComparingTo("7500");
        verify(walletRepository).save(w);
        verify(coinTransactionRepository).save(txCaptor.capture());
        CoinTransaction saved = txCaptor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo("2500");
        assertThat(saved.getAmount().signum()).isPositive();
        assertThat(saved.getBalanceAfter()).isEqualByComparingTo("7500");
        assertThat(saved.getWallet()).isSameAs(w);
        assertThat(saved.getType()).isEqualTo(CoinTxType.TOP_UP);
        assertThat(saved.getReferenceType()).isEqualTo("TOP_UP_REQUEST");
        assertThat(saved.getReferenceId()).isEqualTo(42L);
        assertThat(saved.getNote()).isEqualTo("approved by admin");
        assertThat(tx).isSameAs(saved);
    }

    @Test
    void creditThrowsWhenAmountIsZero() {
        Wallet w = wallet(10L, customer, "5000");

        assertThatThrownBy(() -> walletService.credit(w, BigDecimal.ZERO, CoinTxType.TOP_UP,
                "TOP_UP_REQUEST", 42L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit amount must be positive");

        assertThat(w.getBalance()).isEqualByComparingTo("5000");
        verifyNoInteractions(coinTransactionRepository);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void creditThrowsWhenAmountIsNegative() {
        Wallet w = wallet(10L, customer, "5000");

        assertThatThrownBy(() -> walletService.credit(w, new BigDecimal("-1"), CoinTxType.TOP_UP,
                "TOP_UP_REQUEST", 42L, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(w.getBalance()).isEqualByComparingTo("5000");
        verifyNoInteractions(coinTransactionRepository);
    }

    @Test
    void debitDecreasesBalanceAndWritesNegativeLedgerRow() {
        Wallet w = wallet(10L, customer, "5000");
        echoSavedTransaction();

        walletService.debit(w, new BigDecimal("1200"), CoinTxType.PURCHASE, "ORDER", 99L, "order #99");

        assertThat(w.getBalance()).isEqualByComparingTo("3800");
        verify(coinTransactionRepository).save(txCaptor.capture());
        CoinTransaction saved = txCaptor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo("-1200");
        assertThat(saved.getAmount().signum()).isNegative();
        assertThat(saved.getBalanceAfter()).isEqualByComparingTo("3800");
        assertThat(saved.getType()).isEqualTo(CoinTxType.PURCHASE);
        assertThat(saved.getReferenceType()).isEqualTo("ORDER");
        assertThat(saved.getReferenceId()).isEqualTo(99L);
    }

    @Test
    void debitThrowsWhenBalanceIsInsufficient() {
        Wallet w = wallet(10L, customer, "1000");

        assertThatThrownBy(() -> walletService.debit(w, new BigDecimal("1000.01"), CoinTxType.PURCHASE,
                "ORDER", 99L, null))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient coin balance");

        assertThat(w.getBalance()).isEqualByComparingTo("1000");
        verifyNoInteractions(coinTransactionRepository);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void debitOfExactlyTheFullBalanceSucceedsAndLeavesZero() {
        Wallet w = wallet(10L, customer, "1000");
        echoSavedTransaction();

        walletService.debit(w, new BigDecimal("1000"), CoinTxType.PURCHASE, "ORDER", 99L, null);

        assertThat(w.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(coinTransactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getBalanceAfter()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo("-1000");
    }

    @Test
    void debitThrowsWhenAmountIsZero() {
        Wallet w = wallet(10L, customer, "5000");

        assertThatThrownBy(() -> walletService.debit(w, BigDecimal.ZERO, CoinTxType.PURCHASE,
                "ORDER", 99L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Debit amount must be positive");

        assertThat(w.getBalance()).isEqualByComparingTo("5000");
        verifyNoInteractions(coinTransactionRepository);
    }

    @Test
    void debitThrowsWhenAmountIsNegative() {
        Wallet w = wallet(10L, customer, "5000");

        assertThatThrownBy(() -> walletService.debit(w, new BigDecimal("-500"), CoinTxType.PURCHASE,
                "ORDER", 99L, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(w.getBalance()).isEqualByComparingTo("5000");
        verifyNoInteractions(coinTransactionRepository);
    }

    @Test
    void ledgerSumAlwaysEqualsFinalBalanceAcrossMixedCreditsAndDebits() {
        Wallet w = wallet(10L, customer, "0");
        echoSavedTransaction();

        walletService.credit(w, new BigDecimal("100000"), CoinTxType.TOP_UP, "TOP_UP_REQUEST", 1L, null);
        walletService.credit(w, new BigDecimal("50000"), CoinTxType.TOP_UP, "TOP_UP_REQUEST", 2L, null);
        walletService.debit(w, new BigDecimal("30000"), CoinTxType.PURCHASE, "ORDER", 3L, null);
        walletService.debit(w, new BigDecimal("20000"), CoinTxType.PURCHASE, "ORDER", 4L, null);
        walletService.credit(w, new BigDecimal("7500"), CoinTxType.REFUND, "ORDER", 4L, null);

        verify(coinTransactionRepository, times(5)).save(txCaptor.capture());
        List<CoinTransaction> ledger = txCaptor.getAllValues();

        BigDecimal sum = ledger.stream()
                .map(CoinTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sum).isEqualByComparingTo(w.getBalance());
        assertThat(w.getBalance()).isEqualByComparingTo("107500");
        assertThat(ledger.get(ledger.size() - 1).getBalanceAfter()).isEqualByComparingTo(w.getBalance());
    }

    @Test
    void everyLedgerRowSnapshotsTheRunningBalance() {
        Wallet w = wallet(10L, customer, "0");
        echoSavedTransaction();

        walletService.credit(w, new BigDecimal("1000"), CoinTxType.TOP_UP, "TOP_UP_REQUEST", 1L, null);
        walletService.debit(w, new BigDecimal("400"), CoinTxType.PURCHASE, "ORDER", 2L, null);
        walletService.credit(w, new BigDecimal("250"), CoinTxType.ADJUSTMENT, null, null, "correction");

        verify(coinTransactionRepository, times(3)).save(txCaptor.capture());
        List<CoinTransaction> ledger = txCaptor.getAllValues();

        BigDecimal running = BigDecimal.ZERO;
        for (CoinTransaction tx : ledger) {
            running = running.add(tx.getAmount());
            assertThat(tx.getBalanceAfter()).isEqualByComparingTo(running);
        }
        assertThat(running).isEqualByComparingTo(w.getBalance());
    }

    @Test
    void aRejectedDebitLeavesTheLedgerSumStillEqualToTheBalance() {
        Wallet w = wallet(10L, customer, "0");
        echoSavedTransaction();

        walletService.credit(w, new BigDecimal("1000"), CoinTxType.TOP_UP, "TOP_UP_REQUEST", 1L, null);

        assertThatThrownBy(() -> walletService.debit(w, new BigDecimal("5000"), CoinTxType.PURCHASE,
                "ORDER", 2L, null))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(coinTransactionRepository, times(1)).save(txCaptor.capture());
        BigDecimal sum = txCaptor.getAllValues().stream()
                .map(CoinTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sum).isEqualByComparingTo(w.getBalance());
        assertThat(w.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void getWalletReturnsBalanceWithRecentTransactions() {
        Wallet w = wallet(10L, customer, "5000");
        when(walletRepository.findByUser(customer)).thenReturn(Optional.of(w));
        when(coinTransactionRepository.findTop10ByWalletOrderByCreatedAtDesc(w))
                .thenReturn(List.of(transaction(1L, w, CoinTxType.TOP_UP, "5000", "5000")));

        WalletResponse response = walletService.getWallet(customer);

        assertThat(response.getBalance()).isEqualByComparingTo("5000");
        assertThat(response.getRecentTransactions()).hasSize(1);
        CoinTransactionResponse first = response.getRecentTransactions().get(0);
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getType()).isEqualTo(CoinTxType.TOP_UP);
        assertThat(first.getAmount()).isEqualByComparingTo("5000");
        assertThat(first.getBalanceAfter()).isEqualByComparingTo("5000");
        assertThat(first.getReferenceType()).isEqualTo("TOP_UP_REQUEST");
        assertThat(first.getReferenceId()).isEqualTo(7L);
        assertThat(first.getNote()).isEqualTo("seed");
    }

    @Test
    void getTransactionsThrowsWhenUserHasNoWallet() {
        Pageable pageable = PageRequest.of(0, 10);
        when(walletRepository.findByUser(customer)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getTransactions(customer, pageable))
                .isInstanceOf(WalletNotFoundException.class);

        verifyNoInteractions(coinTransactionRepository);
    }

    @Test
    void getTransactionsMapsPageContent() {
        Wallet w = wallet(10L, customer, "5000");
        Pageable pageable = PageRequest.of(0, 10);
        when(walletRepository.findByUser(customer)).thenReturn(Optional.of(w));
        when(coinTransactionRepository.findByWalletOrderByCreatedAtDesc(w, pageable))
                .thenReturn(new PageImpl<>(List.of(transaction(3L, w, CoinTxType.PURCHASE, "-1200", "3800"))));

        Page<CoinTransactionResponse> page = walletService.getTransactions(customer, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(3L);
        assertThat(page.getContent().get(0).getAmount()).isEqualByComparingTo("-1200");
        assertThat(page.getContent().get(0).getBalanceAfter()).isEqualByComparingTo("3800");
    }

    @Test
    void getWalletOfThrowsWhenTheAccountHoldsNoWallet() {
        when(walletRepository.findByUserId(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWalletOf(55L))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("staff and admin accounts cannot hold one");

        verifyNoInteractions(coinTransactionRepository);
    }

    @Test
    void getWalletOfReturnsTheTargetUsersBalance() {
        Wallet w = wallet(10L, customer, "12345");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(w));
        when(coinTransactionRepository.findTop10ByWalletOrderByCreatedAtDesc(w)).thenReturn(List.of());

        WalletResponse response = walletService.getWalletOf(1L);

        assertThat(response.getBalance()).isEqualByComparingTo("12345");
        assertThat(response.getRecentTransactions()).isEmpty();
    }
}
