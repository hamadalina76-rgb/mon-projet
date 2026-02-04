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
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Wallet - Portefeuille électronique
 */
@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_wallet_user", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers l'utilisateur (auth-service)
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * Solde actuel
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Devise (TND, EUR, USD)
     */
    @Column(length = 3)
    @Builder.Default
    private String currency = "TND";

    /**
     * Solde minimum autorisé (peut être négatif pour certains comptes)
     */
    @Column(name = "minimum_balance", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumBalance = BigDecimal.ZERO;

    /**
     * Limite de recharge par jour
     */
    @Column(name = "daily_top_up_limit", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal dailyTopUpLimit = new BigDecimal("1000.00");

    /**
     * Wallet actif
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Wallet vérifié (KYC passé)
     */
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Dernière transaction
     */
    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    // ==================== RELATIONS ====================

    @OneToMany(mappedBy = "walletId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WalletTransaction> transactions = new ArrayList<>();

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifier si le solde est suffisant
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.subtract(amount).compareTo(minimumBalance) >= 0;
    }

    /**
     * Créditer le wallet
     */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /**
     * Débiter le wallet
     */
    public boolean debit(BigDecimal amount) {
        if (!hasSufficientBalance(amount)) {
            return false;
        }
        this.balance = this.balance.subtract(amount);
        this.lastTransactionAt = LocalDateTime.now();
        return true;
    }
}
