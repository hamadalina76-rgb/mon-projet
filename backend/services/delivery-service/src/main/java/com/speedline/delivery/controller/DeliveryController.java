package com.speedline.delivery.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Delivery
 * 
 * Endpoints:
 * POST   /deliveries - Créer livraison
 * GET    /deliveries/{id} - Détails livraison
 * PUT    /deliveries/{id}/accept - Accepter livraison
 * PUT    /deliveries/{id}/pickup - Récupérer commande
 * PUT    /deliveries/{id}/complete - Terminer livraison
 * POST   /deliveries/{id}/location - Update GPS
 * GET    /deliveries/{id}/track - Tracking
 * POST   /deliveries/{id}/proof - Preuve livraison
 */
@RestController
@RequestMapping("/deliveries")
public class DeliveryController {
    // TODO: Implémenter
}
