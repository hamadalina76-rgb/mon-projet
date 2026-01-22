package com.speedline.order.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Order
 * 
 * Endpoints:
 * POST   /orders - Créer commande
 * GET    /orders/{id} - Détails commande
 * GET    /orders - Liste commandes
 * PUT    /orders/{id}/status - Changer statut
 * DELETE /orders/{id} - Annuler commande
 * GET    /orders/{id}/track - Tracking
 * GET    /customers/{id}/orders - Historique client
 * GET    /partners/{id}/orders - Commandes partenaire
 */
@RestController
@RequestMapping("/orders")
public class OrderController {
    // TODO: Implémenter
}
