package com.speedline.payment.service.impl;

import com.speedline.payment.domain.WalletTransaction.TransactionType;
import com.speedline.payment.repository.WalletRepository;
import com.speedline.payment.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implémentation du service de gestion des wallets
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    // TODO: Injecter WalletTransactionRepository, PaymentService, etc.

    @Override
    @Transactional
    public WalletResponse createWallet(Long userId) {
        // TODO: Implémenter la création d'un wallet
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(Long userId) {
        // TODO: Implémenter la récupération du wallet
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        // TODO: Implémenter la récupération du solde
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public WalletResponse topUp(Long userId, BigDecimal amount, Long paymentMethodId) {
        // TODO: Implémenter le rechargement
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public TransactionResponse pay(Long userId, BigDecimal amount, String referenceId, String description) {
        // TODO: Implémenter le paiement
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public TransactionResponse refund(Long userId, BigDecimal amount, String referenceId, String description) {
        // TODO: Implémenter le remboursement
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public TransactionResponse addBonus(Long userId, BigDecimal amount, String description) {
        // TODO: Implémenter l'ajout de bonus
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long userId, Pageable pageable) {
        // TODO: Implémenter l'historique des transactions
        throw new UnsupportedOperationException("À implémenter");
    }
}
