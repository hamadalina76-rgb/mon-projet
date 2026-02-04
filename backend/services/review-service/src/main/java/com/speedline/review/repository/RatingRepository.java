package com.speedline.review.repository;

import com.speedline.review.domain.Rating;
import com.speedline.review.domain.Rating.TargetType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour Rating (MongoDB)
 */
@Repository
public interface RatingRepository extends MongoRepository<Rating, String> {

    /**
     * Trouver la note agrégée d'une entité
     */
    Optional<Rating> findByEntityTypeAndEntityId(TargetType entityType, Long entityId);

    /**
     * Trouver par clé d'entité
     */
    Optional<Rating> findByEntityKey(String entityKey);

    /**
     * Vérifier si une note existe pour une entité
     */
    boolean existsByEntityTypeAndEntityId(TargetType entityType, Long entityId);
}
