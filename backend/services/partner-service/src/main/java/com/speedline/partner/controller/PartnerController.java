package com.speedline.partner.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Partner
 * 
 * Endpoints:
 * GET    /partners - Liste partenaires
 * GET    /partners/{id} - Détails partenaire
 * POST   /partners - Créer partenaire
 * PUT    /partners/{id} - Modifier partenaire
 * GET    /partners/{id}/menu - Menu complet
 * PUT    /partners/{id}/status - Changer statut
 * GET    /partners/search - Recherche
 */
@RestController
@RequestMapping("/partners")
public class PartnerController {
    // TODO: Implémenter
}
