package com.speedline.payment.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité WalletTransaction - Transactions du wallet
 */
@Entity
@Table(name = "wallet_transactions", indexes = {
    @Index(name = "idx_wallet_tx_wallet", columnList = "wallet_id"),
    @Index(name = "idx_wallet_tx_type", columnList = "type"),
    @Index(name = "idx_wallet_tx_reference", columnList = "reference_id"),
    @Index(name = "idx_wallet_tx_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers le wallet
     */
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    /**
     * Type de transaction
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /**
     * Montant de la transaction (toujours positif)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Solde après la transaction
     */
    @Column(name = "balance_after", precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    /**
     * Description de la transaction
     */
    @Column(length = 500)
    private String description;

    /**
     * ID de référence (orderId, paymentId, etc.)
     */
    @Column(name = "reference_id", length = 255)
    private String referenceId;

    /**
     * Type de référence (ORDER, PAYMENT, REFUND, etc.)
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * Statut de la transaction
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private TransactionStatus status = TransactionStatus.COMPLETED;

    /**
     * Métadonnées (JSON)
     */
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ==================== ENUMS ====================

    public enum TransactionType {
        /**
         * Recharge du wallet
         */
        TOP_UP,
        
        /**
         * Paiement d'une commande
         */
        PAYMENT,
        
        /**
         * Remboursement
         */
        REFUND,
        
        /**
         * Bonus (parrainage, promo, etc.)
         */
        BONUS,
        
        /**
         * Cashback
         */
        CASHBACK,
        
        /**
         * Retrait
         */
        WITHDRAWAL,
        
        /**
         * Ajustement manuel (admin)
         */
        ADJUSTMENT,
        
        /**
         * Conversion de points de fidélité
         */
        LOYALTY_CONVERSION
    }

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifier si c'est un crédit
     */
    public boolean isCredit() {
        return type == TransactionType.TOP_UP ||
               type == TransactionType.REFUND ||
               type == TransactionType.BONUS ||
               type == TransactionType.CASHBACK ||
               type == TransactionType.LOYALTY_CONVERSION ||
               (type == TransactionType.ADJUSTMENT && amount.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Vérifier si c'est un débit
     */
    public boolean isDebit() {
        return type == TransactionType.PAYMENT ||
               type == TransactionType.WITHDRAWAL ||
               (type == TransactionType.ADJUSTMENT && amount.compareTo(BigDecimal.ZERO) < 0);
    }

    /**
     * Créer une transaction de crédit
     */
    public static WalletTransaction credit(Long walletId, TransactionType type, 
                                            BigDecimal amount, String description,
                                            String referenceId, String referenceType) {
        return WalletTransaction.builder()
                .walletId(walletId)
                .type(type)
                .amount(amount)
                .description(description)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .status(TransactionStatus.COMPLETED)
                .build();
    }

    /**
     * Créer une transaction de débit
     */
    public static WalletTransaction debit(Long walletId, TransactionType type,
                                           BigDecimal amount, String description,
                                           String referenceId, String referenceType) {
        return WalletTransaction.builder()
                .walletId(walletId)
                .type(type)
                .amount(amount.abs())
                .description(description)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .status(TransactionStatus.COMPLETED)
                .build();
    }
}
