package com.speedline.payment.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité Payment - Transactions de paiement
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order", columnList = "orderId", unique = true),
    @Index(name = "idx_payment_user", columnList = "userId"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_transaction", columnList = "transactionId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers la commande (order-service)
     */
    @Column(nullable = false, unique = true)
    private Long orderId;

    /**
     * Référence vers l'utilisateur (auth-service)
     */
    @Column(nullable = false)
    private Long userId;

    // ==================== TRANSACTION ====================

    /**
     * ID de la transaction chez le provider (Stripe, etc.)
     */
    @Column(length = 255)
    private String transactionId;

    /**
     * Référence externe (numéro de commande, etc.)
     */
    @Column(length = 100)
    private String externalReference;

    // ==================== MÉTHODE ET PROVIDER ====================

    /**
     * Méthode de paiement utilisée
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    /**
     * Provider de paiement (STRIPE, WALLET, CASH)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    /**
     * ID de la carte utilisée (si paiement par carte)
     */
    private Long paymentMethodId;

    // ==================== MONTANTS ====================

    /**
     * Montant du paiement
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Devise (TND, EUR, USD)
     */
    @Column(length = 3)
    @Builder.Default
    private String currency = "TND";

    /**
     * Montant remboursé
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    /**
     * Frais de transaction (commission du provider)
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal transactionFee = BigDecimal.ZERO;

    // ==================== STATUT ====================

    /**
     * Statut du paiement
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Raison de l'échec (si applicable)
     */
    @Column(length = 500)
    private String failureReason;

    /**
     * Code d'erreur du provider
     */
    @Column(length = 100)
    private String errorCode;

    // ==================== TIMESTAMPS ====================

    /**
     * Date de création
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date de mise à jour
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Date de complétion du paiement
     */
    private LocalDateTime completedAt;

    /**
     * Date du dernier remboursement
     */
    private LocalDateTime lastRefundAt;

    // ==================== MÉTADONNÉES ====================

    /**
     * Adresse IP du client
     */
    @Column(length = 50)
    private String clientIp;

    /**
     * User agent du client
     */
    @Column(length = 500)
    private String userAgent;

    /**
     * Données additionnelles (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    // ==================== ENUMS ====================

    public enum PaymentMethod {
        CARD,
        WALLET,
        CASH,
        CARD_ON_DELIVERY
    }

    public enum PaymentProvider {
        STRIPE,
        WALLET,
        CASH,
        FLOUCI,
        PAYMEE
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifier si le paiement peut être remboursé
     */
    public boolean isRefundable() {
        return status == PaymentStatus.COMPLETED &&
               amount.subtract(refundedAmount).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Obtenir le montant restant remboursable
     */
    public BigDecimal getRefundableAmount() {
        return amount.subtract(refundedAmount);
    }

    /**
     * Marquer comme complété
     */
    public void markAsCompleted(String transactionId) {
        this.transactionId = transactionId;
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Marquer comme échoué
     */
    public void markAsFailed(String reason, String errorCode) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.errorCode = errorCode;
    }

    /**
     * Ajouter un remboursement
     */
    public void addRefund(BigDecimal amount) {
        this.refundedAmount = this.refundedAmount.add(amount);
        this.lastRefundAt = LocalDateTime.now();
        if (this.refundedAmount.compareTo(this.amount) >= 0) {
            this.status = PaymentStatus.REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }
    }
}
