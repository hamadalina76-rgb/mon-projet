package com.speedline.payment.service.impl;

import com.speedline.payment.domain.Payment.PaymentMethod;
import com.speedline.payment.domain.PaymentStatus;
import com.speedline.payment.repository.PaymentRepository;
import com.speedline.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Implémentation du service de traitement des paiements
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    // TODO: Injecter StripeService, WalletService, etc.

    @Override
    @Transactional
    public PaymentResponse processPayment(Long orderId, Long userId, BigDecimal amount,
                                         PaymentMethod method, Long paymentMethodId) {
        // TODO: Implémenter le traitement du paiement
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        // TODO: Implémenter la récupération par ID
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        // TODO: Implémenter la récupération par orderId
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        // TODO: Implémenter la récupération des paiements d'un utilisateur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatus checkPaymentStatus(Long paymentId) {
        // TODO: Implémenter la vérification du statut
        throw new UnsupportedOperationException("À implémenter");
    }
}
