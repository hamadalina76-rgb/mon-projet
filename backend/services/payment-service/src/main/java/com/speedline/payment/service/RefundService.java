package com.speedline.payment.service;

import java.math.BigDecimal;

/**
 * Service pour la gestion des remboursements
 */
public interface RefundService {

    /**
     * Rembourser intégralement un paiement
     * 
     * @param paymentId ID du paiement
     * @param reason Raison du remboursement
     * @return RefundResponse avec statut
     * @throws PaymentNotFoundException si le paiement n'existe pas
     * @throws PaymentNotRefundableException si le paiement ne peut pas être remboursé
     */
    RefundResponse refundPayment(Long paymentId, String reason);

    /**
     * Rembourser partiellement un paiement
     * 
     * @param paymentId ID du paiement
     * @param amount Montant à rembourser
     * @param reason Raison du remboursement
     * @return RefundResponse avec statut
     * @throws PaymentNotFoundException si le paiement n'existe pas
     * @throws InvalidRefundAmountException si le montant est invalide
     */
    RefundResponse partialRefund(Long paymentId, BigDecimal amount, String reason);

    /**
     * Vérifier si un paiement peut être remboursé
     */
    boolean isRefundable(Long paymentId);

    /**
     * Obtenir le montant remboursable
     */
    BigDecimal getRefundableAmount(Long paymentId);

    /**
     * DTO de réponse pour les remboursements
     */
    record RefundResponse(
            Long paymentId,
            BigDecimal refundedAmount,
            BigDecimal totalRefunded,
            BigDecimal remainingAmount,
            String status,
            String transactionId,
            java.time.LocalDateTime refundedAt
    ) {}
}
