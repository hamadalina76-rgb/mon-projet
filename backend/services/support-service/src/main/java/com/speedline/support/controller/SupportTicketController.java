package com.speedline.support.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Support Tickets
 * 
 * Endpoints:
 * POST   /tickets - Créer ticket
 * GET    /tickets/{id} - Détails ticket
 * GET    /tickets/user/{userId} - Tickets utilisateur
 * PUT    /tickets/{id}/status - Changer statut
 * POST   /tickets/{id}/messages - Ajouter message
 * GET    /tickets/{id}/messages - Historique messages
 */
@RestController
@RequestMapping("/tickets")
public class SupportTicketController {
    // TODO: Implémenter
}
