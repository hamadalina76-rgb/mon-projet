package com.speedline.payment.service.impl;

import com.speedline.payment.repository.PaymentRepository;
import com.speedline.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Implémentation du service de gestion des remboursements
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefundServiceImpl implements RefundService {

    private final PaymentRepository paymentRepository;
    // TODO: Injecter StripeService, WalletService, etc.

    @Override
    @Transactional
    public RefundResponse refundPayment(Long paymentId, String reason) {
        // TODO: Implémenter le remboursement intégral
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public RefundResponse partialRefund(Long paymentId, BigDecimal amount, String reason) {
        // TODO: Implémenter le remboursement partiel
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRefundable(Long paymentId) {
        // TODO: Implémenter la vérification de remboursabilité
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRefundableAmount(Long paymentId) {
        // TODO: Implémenter la récupération du montant remboursable
        throw new UnsupportedOperationException("À implémenter");
    }
}
