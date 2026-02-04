package com.speedline.review.service.impl;

import com.speedline.review.domain.Review.TargetType;
import com.speedline.review.repository.ReviewRepository;
import com.speedline.review.repository.RatingRepository;
import com.speedline.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implémentation du service de gestion des avis
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RatingRepository ratingRepository;

    @Override
    @Transactional
    public ReviewDTO createReview(Long orderId, Long customerId, TargetType targetType,
                                 Long targetId, Integer rating, String comment, List<String> imageUrls) {
        // TODO: Implémenter la création d'un avis
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDTO getReviewById(String reviewId) {
        // TODO: Implémenter la récupération par ID
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getPartnerReviews(Long partnerId, Pageable pageable) {
        // TODO: Implémenter la récupération des avis d'un partenaire
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getCourierReviews(Long courierId, Pageable pageable) {
        // TODO: Implémenter la récupération des avis d'un livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDTO getOrderReview(Long orderId) {
        // TODO: Implémenter la récupération de l'avis d'une commande
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public ReviewDTO respondToReview(String reviewId, Long respondedBy, String response) {
        // TODO: Implémenter la réponse à un avis
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void deleteReview(String reviewId) {
        // TODO: Implémenter la suppression
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public RatingDTO getAverageRating(TargetType targetType, Long targetId) {
        // TODO: Implémenter la récupération de la note moyenne
        throw new UnsupportedOperationException("À implémenter");
    }
}
