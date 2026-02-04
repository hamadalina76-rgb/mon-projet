package com.speedline.payment.service;

import com.speedline.payment.domain.Payment.PaymentMethod;
import com.speedline.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Service pour le traitement des paiements
 */
public interface PaymentService {

    /**
     * Traiter un paiement pour une commande
     * 
     * @param orderId ID de la commande
     * @param userId ID de l'utilisateur
     * @param amount Montant à payer
     * @param method Méthode de paiement (CARD, WALLET, CASH)
     * @param paymentMethodId ID de la carte (si CARD)
     * @return PaymentResponse avec transactionId, status
     * @throws OrderNotFoundException si la commande n'existe pas
     * @throws PaymentMethodNotFoundException si la carte n'existe pas
     * @throws InsufficientBalanceException si wallet insuffisant
     * @throws PaymentFailedException si le paiement échoue
     */
    PaymentResponse processPayment(Long orderId, Long userId, BigDecimal amount,
                                    PaymentMethod method, Long paymentMethodId);

    /**
     * Récupérer un paiement par ID
     */
    PaymentResponse getPaymentById(Long paymentId);

    /**
     * Récupérer un paiement par commande
     */
    PaymentResponse getPaymentByOrderId(Long orderId);

    /**
     * Récupérer l'historique des paiements d'un utilisateur
     */
    Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable);

    /**
     * Vérifier le statut d'un paiement
     */
    PaymentStatus checkPaymentStatus(Long paymentId);

    /**
     * DTO de réponse pour les paiements
     */
    record PaymentResponse(
            Long id,
            Long orderId,
            String transactionId,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            PaymentStatus status,
            String failureReason,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime completedAt
    ) {}
}
