package com.speedline.review.service;

import com.speedline.review.domain.Review.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service pour la gestion des avis
 */
public interface ReviewService {

    /**
     * Créer un avis
     */
    ReviewDTO createReview(Long orderId, Long customerId, TargetType targetType, 
                           Long targetId, Integer rating, String comment, List<String> imageUrls);

    /**
     * Récupérer un avis par ID
     */
    ReviewDTO getReviewById(String reviewId);

    /**
     * Récupérer les avis d'un partenaire
     */
    Page<ReviewDTO> getPartnerReviews(Long partnerId, Pageable pageable);

    /**
     * Récupérer les avis d'un livreur
     */
    Page<ReviewDTO> getCourierReviews(Long courierId, Pageable pageable);

    /**
     * Récupérer l'avis d'une commande
     */
    ReviewDTO getOrderReview(Long orderId);

    /**
     * Récupérer les avis d'un client (paginé, pour admin - fiche client)
     */
    Page<ReviewDTO> getCustomerReviews(Long customerId, Pageable pageable);

    /**
     * Répondre à un avis
     */
    ReviewDTO respondToReview(String reviewId, Long respondedBy, String response);

    /**
     * Supprimer un avis
     */
    void deleteReview(String reviewId);

    /**
     * Obtenir la note moyenne d'une entité
     */
    RatingDTO getAverageRating(TargetType targetType, Long targetId);

    /**
     * DTO pour les avis
     */
    record ReviewDTO(
            String id,
            Long orderId,
            Long customerId,
            String customerName,
            TargetType targetType,
            Long targetId,
            Integer rating,
            String comment,
            List<String> imageUrls,
            String response,
            java.time.LocalDateTime responseDate,
            Boolean isVerified,
            java.time.LocalDateTime createdAt
    ) {}

    /**
     * DTO pour les notes agrégées
     */
    record RatingDTO(
            TargetType targetType,
            Long targetId,
            Double averageRating,
            Integer totalReviews,
            Integer fiveStars,
            Integer fourStars,
            Integer threeStars,
            Integer twoStars,
            Integer oneStar
    ) {}
}
