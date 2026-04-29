package com.speedline.payment.service.impl;

import com.speedline.payment.domain.Wallet;
import com.speedline.payment.domain.WalletTransaction;
import com.speedline.payment.domain.WalletTransaction.TransactionType;
import com.speedline.payment.repository.WalletRepository;
import com.speedline.payment.service.WalletService.TransactionResponse;
import com.speedline.payment.service.WalletService.WalletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WalletServiceImpl.
 *
 * All service methods are currently TODO stubs that throw UnsupportedOperationException.
 * The tests document the expected contract and include Wallet entity helper tests.
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet sampleWallet;

    @BeforeEach
    void setUp() {
        sampleWallet = Wallet.builder()
                .id(1L)
                .userId(42L)
                .balance(new BigDecimal("100.00"))
                .currency("TND")
                .isActive(true)
                .isVerified(false)
                .build();
    }

    // ==================== createWallet ====================

    @Nested
    @DisplayName("createWallet")
    class CreateWallet {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void createWallet_throwsUnsupported() {
            assertThatThrownBy(() -> walletService.createWallet(42L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== getWalletByUserId ====================

    @Nested
    @DisplayName("getWalletByUserId")
    class GetWalletByUserId {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void getWalletByUserId_throwsUnsupported() {
            assertThatThrownBy(() -> walletService.getWalletByUserId(42L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== getBalance ====================

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void getBalance_throwsUnsupported() {
            assertThatThrownBy(() -> walletService.getBalance(42L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== topUp ====================

    @Nested
    @DisplayName("topUp")
    class TopUp {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void topUp_throwsUnsupported() {
            assertThatThrownBy(() -> walletService.topUp(42L, new BigDecimal("50.00"), 1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== pay ====================

    @Nested
    @DisplayName("pay")
    class Pay {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void pay_throwsUnsupported() {
            assertThatThrownBy(() ->
                    walletService.pay(42L, new BigDecimal("20.00"), "ORDER-100", "Order payment"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== refund ====================

    @Nested
    @DisplayName("refund")
    class Refund {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void refund_throwsUnsupported() {
            assertThatThrownBy(() ->
                    walletService.refund(42L, new BigDecimal("10.00"), "ORDER-100", "Order refund"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== addBonus ====================

    @Nested
    @DisplayName("addBonus")
    class AddBonus {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void addBonus_throwsUnsupported() {
            assertThatThrownBy(() ->
                    walletService.addBonus(42L, new BigDecimal("5.00"), "Referral bonus"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== getTransactionHistory ====================

    @Nested
    @DisplayName("getTransactionHistory")
    class GetTransactionHistory {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void getTransactionHistory_throwsUnsupported() {
            Pageable pageable = PageRequest.of(0, 20);
            assertThatThrownBy(() -> walletService.getTransactionHistory(42L, pageable))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== Wallet entity helpers ====================

    @Nested
    @DisplayName("Wallet entity helpers")
    class WalletEntityHelpers {

        @Test
        @DisplayName("hasSufficientBalance returns true when balance covers amount")
        void hasSufficientBalance_true() {
            assertThat(sampleWallet.hasSufficientBalance(new BigDecimal("100.00"))).isTrue();
        }

        @Test
        @DisplayName("hasSufficientBalance returns false when balance is insufficient")
        void hasSufficientBalance_false() {
            assertThat(sampleWallet.hasSufficientBalance(new BigDecimal("100.01"))).isFalse();
        }

        @Test
        @DisplayName("hasSufficientBalance accounts for minimum balance")
        void hasSufficientBalance_withMinimumBalance() {
            sampleWallet.setMinimumBalance(new BigDecimal("20.00"));
            // balance 100, min 20 -> effective available = 80
            assertThat(sampleWallet.hasSufficientBalance(new BigDecimal("80.00"))).isTrue();
            assertThat(sampleWallet.hasSufficientBalance(new BigDecimal("80.01"))).isFalse();
        }

        @Test
        @DisplayName("credit increases balance")
        void credit_increasesBalance() {
            sampleWallet.credit(new BigDecimal("50.00"));
            assertThat(sampleWallet.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(sampleWallet.getLastTransactionAt()).isNotNull();
        }

        @Test
        @DisplayName("debit decreases balance when sufficient")
        void debit_decreasesBalance() {
            boolean result = sampleWallet.debit(new BigDecimal("30.00"));
            assertThat(result).isTrue();
            assertThat(sampleWallet.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
        }

        @Test
        @DisplayName("debit returns false when insufficient balance")
        void debit_returnsFalse_whenInsufficient() {
            boolean result = sampleWallet.debit(new BigDecimal("200.00"));
            assertThat(result).isFalse();
            assertThat(sampleWallet.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("debit to zero succeeds")
        void debit_toZero() {
            boolean result = sampleWallet.debit(new BigDecimal("100.00"));
            assertThat(result).isTrue();
            assertThat(sampleWallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ==================== WalletTransaction entity helpers ====================

    @Nested
    @DisplayName("WalletTransaction entity helpers")
    class WalletTransactionHelpers {

        @Test
        @DisplayName("isCredit returns true for TOP_UP")
        void isCredit_topUp() {
            WalletTransaction tx = WalletTransaction.builder()
                    .type(TransactionType.TOP_UP)
                    .amount(BigDecimal.TEN)
                    .build();
            assertThat(tx.isCredit()).isTrue();
            assertThat(tx.isDebit()).isFalse();
        }

        @Test
        @DisplayName("isCredit returns true for REFUND")
        void isCredit_refund() {
            WalletTransaction tx = WalletTransaction.builder()
                    .type(TransactionType.REFUND)
                    .amount(BigDecimal.TEN)
                    .build();
            assertThat(tx.isCredit()).isTrue();
        }

        @Test
        @DisplayName("isCredit returns true for BONUS")
        void isCredit_bonus() {
            WalletTransaction tx = WalletTransaction.builder()
                    .type(TransactionType.BONUS)
                    .amount(BigDecimal.TEN)
                    .build();
            assertThat(tx.isCredit()).isTrue();
        }

        @Test
        @DisplayName("isDebit returns true for PAYMENT")
        void isDebit_payment() {
            WalletTransaction tx = WalletTransaction.builder()
                    .type(TransactionType.PAYMENT)
                    .amount(BigDecimal.TEN)
                    .build();
            assertThat(tx.isDebit()).isTrue();
            assertThat(tx.isCredit()).isFalse();
        }

        @Test
        @DisplayName("isDebit returns true for WITHDRAWAL")
        void isDebit_withdrawal() {
            WalletTransaction tx = WalletTransaction.builder()
                    .type(TransactionType.WITHDRAWAL)
                    .amount(BigDecimal.TEN)
                    .build();
            assertThat(tx.isDebit()).isTrue();
        }

        @Test
        @DisplayName("static credit factory creates correct transaction")
        void creditFactory() {
            WalletTransaction tx = WalletTransaction.credit(
                    1L, TransactionType.TOP_UP, new BigDecimal("50.00"),
                    "Top up", "REF-1", "PAYMENT");
            assertThat(tx.getWalletId()).isEqualTo(1L);
            assertThat(tx.getType()).isEqualTo(TransactionType.TOP_UP);
            assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(tx.getStatus()).isEqualTo(WalletTransaction.TransactionStatus.COMPLETED);
        }

        @Test
        @DisplayName("static debit factory creates transaction with absolute amount")
        void debitFactory() {
            WalletTransaction tx = WalletTransaction.debit(
                    1L, TransactionType.PAYMENT, new BigDecimal("-20.00"),
                    "Order payment", "ORDER-100", "ORDER");
            assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        }
    }
}
