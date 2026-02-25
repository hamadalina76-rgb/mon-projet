package com.speedline.review.repository;

import com.speedline.review.domain.Review;
import com.speedline.review.domain.Review.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour Review (MongoDB)
 */
@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    /**
     * Trouver un avis par ID de commande
     */
    Optional<Review> findByOrderId(Long orderId);

    /**
     * Trouver les avis d'une entité (partenaire, livreur, etc.)
     */
    Page<Review> findByTargetTypeAndTargetIdAndIsVisibleTrue(TargetType targetType, Long targetId, Pageable pageable);

    /**
     * Trouver les avis d'un client
     */
    List<Review> findByCustomerId(Long customerId);

    /**
     * Trouver les avis d'un client avec pagination (pour admin - fiche client)
     */
    Page<Review> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Trouver les avis vérifiés d'une entité
     */
    @Query("{ 'targetType': ?0, 'targetId': ?1, 'isVerified': true, 'isVisible': true }")
    List<Review> findVerifiedReviews(TargetType targetType, Long targetId);

    /**
     * Compter les avis d'une entité
     */
    long countByTargetTypeAndTargetIdAndIsVisibleTrue(TargetType targetType, Long targetId);

    /**
     * Trouver les avis avec une note spécifique
     */
    List<Review> findByTargetTypeAndTargetIdAndRatingAndIsVisibleTrue(TargetType targetType, Long targetId, Integer rating);

    /**
     * Trouver les avis récents
     */
    @Query("{ 'targetType': ?0, 'targetId': ?1, 'isVisible': true }")
    Page<Review> findRecentReviews(TargetType targetType, Long targetId, Pageable pageable);
}
