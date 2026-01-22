package com.speedline.payment.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Wallet
 * 
 * Endpoints:
 * GET    /wallets/{userId} - Solde wallet
 * POST   /wallets/{userId}/add - Ajouter fonds
 * POST   /wallets/{userId}/deduct - Déduire fonds
 * GET    /payment-methods/{userId} - Moyens de paiement
 * POST   /payment-methods - Ajouter carte
 * DELETE /payment-methods/{id} - Supprimer carte
 */
@RestController
@RequestMapping("/wallets")
public class WalletController {
    // TODO: Implémenter
}
