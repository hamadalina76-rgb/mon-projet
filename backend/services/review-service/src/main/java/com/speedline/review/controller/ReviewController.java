package com.speedline.review.controller;

import com.speedline.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Review
 *
 * Endpoints:
 * POST   /reviews - Créer avis
 * GET    /reviews/{id} - Détails avis
 * GET    /reviews/partner/{partnerId} - Avis partenaire
 * GET    /reviews/courier/{courierId} - Avis livreur
 * GET    /reviews/customer/{customerId} - Avis d'un client (paginé, pour admin)
 * GET    /reviews/order/{orderId} - Avis commande
 * PUT    /reviews/{id}/response - Répondre à avis
 * DELETE /reviews/{id} - Supprimer avis
 * GET    /ratings/partner/{partnerId} - Rating partenaire
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Avis laissés par un client (pour admin - fiche client).
     * GET /reviews/customer/{customerId}?page=0&size=20
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<ReviewService.ReviewDTO>> getCustomerReviews(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(reviewService.getCustomerReviews(customerId, pageable));
    }
}
