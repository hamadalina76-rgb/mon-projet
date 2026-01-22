package com.speedline.review.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Review
 * 
 * Endpoints:
 * POST   /reviews - Créer avis
 * GET    /reviews/{id} - Détails avis
 * GET    /reviews/partner/{partnerId} - Avis partenaire
 * GET    /reviews/courier/{courierId} - Avis livreur
 * GET    /reviews/order/{orderId} - Avis commande
 * PUT    /reviews/{id}/response - Répondre à avis
 * DELETE /reviews/{id} - Supprimer avis
 * GET    /ratings/partner/{partnerId} - Rating partenaire
 */
@RestController
@RequestMapping("/reviews")
public class ReviewController {
    // TODO: Implémenter
}
