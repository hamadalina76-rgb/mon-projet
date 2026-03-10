package com.speedline.order.controller;

import com.speedline.order.dto.OrderResponse;
import com.speedline.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
 * GET    /orders/customers/{customerId}/orders - Historique client (paginé)
 * GET    /partners/{id}/orders - Commandes partenaire
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Historique des commandes d'un client (pour admin panel - fiche client).
     * GET /orders/customers/{customerId}/orders?page=0&size=20
     */
    @GetMapping("/customers/{customerId}/orders")
    public ResponseEntity<Page<OrderResponse>> getCustomerOrders(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getCustomerOrders(customerId, pageable));
    }

    /**
     * Stats journalières de commandes pour une liste de partenaires (endpoint interne).
     * Consommé par partner-service via Feign pour les statistiques des catégories.
     * GET /orders/internal/stats/daily?partnerIds=1,2,3&days=30
     */
    @GetMapping("/internal/stats/daily")
    public ResponseEntity<Map<String, Long>> getDailyStatsByPartners(
            @RequestParam List<Long> partnerIds,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(orderService.getDailyStatsByPartners(partnerIds, days));
    }
}
