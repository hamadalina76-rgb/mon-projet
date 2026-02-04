package com.speedline.payment.service;

import com.speedline.payment.domain.WalletTransaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour la gestion des wallets
 */
public interface WalletService {

    /**
     * Créer un wallet pour un utilisateur
     */
    WalletResponse createWallet(Long userId);

    /**
     * Récupérer le wallet d'un utilisateur
     */
    WalletResponse getWalletByUserId(Long userId);

    /**
     * Obtenir le solde d'un utilisateur
     */
    BigDecimal getBalance(Long userId);

    /**
     * Recharger le wallet (top-up)
     */
    WalletResponse topUp(Long userId, BigDecimal amount, Long paymentMethodId);

    /**
     * Payer avec le wallet
     */
    TransactionResponse pay(Long userId, BigDecimal amount, String referenceId, String description);

    /**
     * Rembourser vers le wallet
     */
    TransactionResponse refund(Long userId, BigDecimal amount, String referenceId, String description);

    /**
     * Ajouter un bonus (parrainage, promo)
     */
    TransactionResponse addBonus(Long userId, BigDecimal amount, String description);

    /**
     * Obtenir l'historique des transactions
     */
    Page<TransactionResponse> getTransactionHistory(Long userId, Pageable pageable);

    /**
     * DTO de réponse pour le wallet
     */
    record WalletResponse(
            Long id,
            Long userId,
            BigDecimal balance,
            String currency,
            Boolean isActive,
            Boolean isVerified
    ) {}

    /**
     * DTO de réponse pour les transactions
     */
    record TransactionResponse(
            Long id,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String description,
            String referenceId,
            java.time.LocalDateTime createdAt
    ) {}
}
