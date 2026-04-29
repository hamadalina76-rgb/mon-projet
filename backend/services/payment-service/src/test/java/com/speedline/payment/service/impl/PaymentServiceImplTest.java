package com.speedline.payment.service.impl;

import com.speedline.payment.domain.Payment;
import com.speedline.payment.domain.Payment.PaymentMethod;
import com.speedline.payment.domain.PaymentStatus;
import com.speedline.payment.repository.PaymentRepository;
import com.speedline.payment.service.PaymentService.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceImpl.
 *
 * Note: The current implementation throws UnsupportedOperationException for all methods
 * (TODO stubs). These tests verify the current behaviour and are structured so that
 * once the real logic is implemented, the "happy path" tests can be updated to assert
 * the expected results while the mocking setup remains valid.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .userId(42L)
                .transactionId("txn_abc123")
                .amount(new BigDecimal("25.50"))
                .currency("TND")
                .method(PaymentMethod.CARD)
                .provider(Payment.PaymentProvider.STRIPE)
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }

    // ==================== processPayment ====================

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void processPayment_throwsUnsupported() {
            assertThatThrownBy(() ->
                    paymentService.processPayment(100L, 42L, new BigDecimal("25.50"),
                            PaymentMethod.CARD, 1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should not interact with repository when not yet implemented")
        void processPayment_noRepositoryInteraction() {
            try {
                paymentService.processPayment(100L, 42L, BigDecimal.TEN, PaymentMethod.WALLET, null);
            } catch (UnsupportedOperationException ignored) {
                // expected
            }
            verifyNoInteractions(paymentRepository);
        }
    }

    // ==================== getPaymentById ====================

    @Nested
    @DisplayName("getPaymentById")
    class GetPaymentById {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void getPaymentById_throwsUnsupported() {
            assertThatThrownBy(() -> paymentService.getPaymentById(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should not call repository when not yet implemented")
        void getPaymentById_noRepositoryCall() {
            try {
                paymentService.getPaymentById(1L);
            } catch (UnsupportedOperationException ignored) {
                // expected
            }
            verify(paymentRepository, never()).findById(anyLong());
        }
    }

    // ==================== getPaymentByOrderId ====================

    @Nested
    @DisplayName("getPaymentByOrderId")
    class GetPaymentByOrderId {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void getPaymentByOrderId_throwsUnsupported() {
            assertThatThrownBy(() -> paymentService.getPaymentByOrderId(100L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== getUserPayments ====================

    @Nested
    @DisplayName("getUserPayments")
    class GetUserPayments {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void getUserPayments_throwsUnsupported() {
            Pageable pageable = PageRequest.of(0, 10);
            assertThatThrownBy(() -> paymentService.getUserPayments(42L, pageable))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== checkPaymentStatus ====================

    @Nested
    @DisplayName("checkPaymentStatus")
    class CheckPaymentStatus {

        @Test
        @DisplayName("should throw UnsupportedOperationException (TODO stub)")
        void checkPaymentStatus_throwsUnsupported() {
            assertThatThrownBy(() -> paymentService.checkPaymentStatus(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== Payment domain helpers (exercised through the entity) ====================

    @Nested
    @DisplayName("Payment entity helpers")
    class PaymentEntityHelpers {

        @Test
        @DisplayName("isRefundable returns true for completed payment with remaining amount")
        void isRefundable_completed_withRemainingAmount() {
            samplePayment.setStatus(PaymentStatus.COMPLETED);
            samplePayment.setRefundedAmount(BigDecimal.ZERO);
            assertThat(samplePayment.isRefundable()).isTrue();
        }

        @Test
        @DisplayName("isRefundable returns false for failed payment")
        void isRefundable_failed() {
            samplePayment.setStatus(PaymentStatus.FAILED);
            assertThat(samplePayment.isRefundable()).isFalse();
        }

        @Test
        @DisplayName("isRefundable returns false when fully refunded")
        void isRefundable_fullyRefunded() {
            samplePayment.setStatus(PaymentStatus.COMPLETED);
            samplePayment.setRefundedAmount(samplePayment.getAmount());
            assertThat(samplePayment.isRefundable()).isFalse();
        }

        @Test
        @DisplayName("getRefundableAmount computes correctly")
        void getRefundableAmount_correct() {
            samplePayment.setRefundedAmount(new BigDecimal("10.00"));
            BigDecimal refundable = samplePayment.getRefundableAmount();
            assertThat(refundable).isEqualByComparingTo(new BigDecimal("15.50"));
        }

        @Test
        @DisplayName("markAsCompleted sets status and transactionId")
        void markAsCompleted_setsFields() {
            samplePayment.setStatus(PaymentStatus.PENDING);
            samplePayment.markAsCompleted("txn_xyz");
            assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(samplePayment.getTransactionId()).isEqualTo("txn_xyz");
            assertThat(samplePayment.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("markAsFailed sets failure reason and error code")
        void markAsFailed_setsFields() {
            samplePayment.markAsFailed("Card declined", "card_declined");
            assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(samplePayment.getFailureReason()).isEqualTo("Card declined");
            assertThat(samplePayment.getErrorCode()).isEqualTo("card_declined");
        }

        @Test
        @DisplayName("addRefund partial sets PARTIALLY_REFUNDED status")
        void addRefund_partial() {
            samplePayment.setRefundedAmount(BigDecimal.ZERO);
            samplePayment.setStatus(PaymentStatus.COMPLETED);
            samplePayment.addRefund(new BigDecimal("10.00"));
            assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(samplePayment.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("addRefund full sets REFUNDED status")
        void addRefund_full() {
            samplePayment.setRefundedAmount(BigDecimal.ZERO);
            samplePayment.setStatus(PaymentStatus.COMPLETED);
            samplePayment.addRefund(samplePayment.getAmount());
            assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }
    }
}
