package com.speedline.payment.service.impl;

import com.speedline.payment.domain.Payment;
import com.speedline.payment.domain.Payment.PaymentMethod;
import com.speedline.payment.domain.PaymentStatus;
import com.speedline.payment.repository.PaymentRepository;
import com.speedline.payment.service.RefundService.RefundResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefundServiceImpl.
 *
 * All service methods are currently TODO stubs that throw UnsupportedOperationException.
 * The tests document the expected contract and will pass once the stubs are removed.
 */
@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private RefundServiceImpl refundService;

    private Payment completedPayment;
    private Payment failedPayment;

    @BeforeEach
    void setUp() {
        completedPayment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .userId(42L)
                .transactionId("txn_abc123")
                .amount(new BigDecimal("50.00"))
                .currency("TND")
                .method(PaymentMethod.CARD)
                .provider(Payment.PaymentProvider.STRIPE)
                .status(PaymentStatus.COMPLETED)
                .refundedAmount(BigDecimal.ZERO)
                .build();

        failedPayment = Payment.builder()
                .id(2L)
                .orderId(200L)
                .userId(43L)
                .amount(new BigDecimal("30.00"))
                .method(PaymentMethod.WALLET)
                .provider(Payment.PaymentProvider.WALLET)
                .status(PaymentStatus.FAILED)
                .refundedAmount(BigDecimal.ZERO)
                .build();
    }

    // ==================== refundPayment ====================

    @Nested
    @DisplayName("refundPayment (full refund)")
    class RefundPayment {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void refundPayment_throwsUnsupported() {
            assertThatThrownBy(() -> refundService.refundPayment(1L, "Customer request"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should not call repository when not yet implemented")
        void refundPayment_noRepositoryCall() {
            try {
                refundService.refundPayment(1L, "Customer request");
            } catch (UnsupportedOperationException ignored) {
                // expected
            }
            verify(paymentRepository, never()).findById(anyLong());
        }
    }

    // ==================== partialRefund ====================

    @Nested
    @DisplayName("partialRefund")
    class PartialRefund {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void partialRefund_throwsUnsupported() {
            assertThatThrownBy(() ->
                    refundService.partialRefund(1L, new BigDecimal("20.00"), "Partial refund"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should throw with zero amount")
        void partialRefund_zeroAmount() {
            assertThatThrownBy(() ->
                    refundService.partialRefund(1L, BigDecimal.ZERO, "Zero refund"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should throw with negative amount")
        void partialRefund_negativeAmount() {
            assertThatThrownBy(() ->
                    refundService.partialRefund(1L, new BigDecimal("-10.00"), "Negative"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== isRefundable ====================

    @Nested
    @DisplayName("isRefundable")
    class IsRefundable {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void isRefundable_throwsUnsupported() {
            assertThatThrownBy(() -> refundService.isRefundable(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== getRefundableAmount ====================

    @Nested
    @DisplayName("getRefundableAmount")
    class GetRefundableAmount {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void getRefundableAmount_throwsUnsupported() {
            assertThatThrownBy(() -> refundService.getRefundableAmount(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== Payment domain refund logic ====================

    @Nested
    @DisplayName("Payment entity refund logic")
    class PaymentRefundLogic {

        @Test
        @DisplayName("completed payment is refundable")
        void completedPayment_isRefundable() {
            assertThat(completedPayment.isRefundable()).isTrue();
        }

        @Test
        @DisplayName("failed payment is not refundable")
        void failedPayment_isNotRefundable() {
            assertThat(failedPayment.isRefundable()).isFalse();
        }

        @Test
        @DisplayName("pending payment is not refundable")
        void pendingPayment_isNotRefundable() {
            completedPayment.setStatus(PaymentStatus.PENDING);
            assertThat(completedPayment.isRefundable()).isFalse();
        }

        @Test
        @DisplayName("fully refunded payment is not refundable")
        void fullyRefundedPayment_isNotRefundable() {
            completedPayment.setRefundedAmount(completedPayment.getAmount());
            assertThat(completedPayment.isRefundable()).isFalse();
        }

        @Test
        @DisplayName("refundable amount for fresh payment is full amount")
        void refundableAmount_freshPayment() {
            assertThat(completedPayment.getRefundableAmount())
                    .isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("refundable amount decreases after partial refund")
        void refundableAmount_afterPartialRefund() {
            completedPayment.addRefund(new BigDecimal("20.00"));
            assertThat(completedPayment.getRefundableAmount())
                    .isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("refundable amount is zero after full refund")
        void refundableAmount_afterFullRefund() {
            completedPayment.addRefund(new BigDecimal("50.00"));
            assertThat(completedPayment.getRefundableAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("addRefund accumulates across multiple calls")
        void addRefund_accumulates() {
            completedPayment.addRefund(new BigDecimal("10.00"));
            assertThat(completedPayment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);

            completedPayment.addRefund(new BigDecimal("40.00"));
            assertThat(completedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(completedPayment.getRefundedAmount())
                    .isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("addRefund sets lastRefundAt timestamp")
        void addRefund_setsTimestamp() {
            assertThat(completedPayment.getLastRefundAt()).isNull();
            completedPayment.addRefund(BigDecimal.ONE);
            assertThat(completedPayment.getLastRefundAt()).isNotNull();
        }
    }
}
