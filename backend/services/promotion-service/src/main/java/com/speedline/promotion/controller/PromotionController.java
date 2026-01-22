package com.speedline.promotion.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Promotion
 * 
 * Endpoints:
 * GET    /promotions/active - Promotions actives
 * POST   /promotions/validate - Valider code promo
 * POST   /promotions/{code}/apply - Appliquer promo
 * GET    /users/{userId}/promotions - Promos utilisateur
 * POST   /promotions - Créer promo (admin)
 * PUT    /promotions/{id} - Modifier promo
 * DELETE /promotions/{id} - Supprimer promo
 */
@RestController
@RequestMapping("/promotions")
public class PromotionController {
    // TODO: Implémenter
}
