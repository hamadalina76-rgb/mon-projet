package com.speedline.review.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entité Rating - Notes agrégées (MongoDB)
 * Stocke les statistiques de notes pour partenaires et livreurs
 */
@Document(collection = "ratings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    private String id;

    @Indexed(unique = true)
    private String entityKey; // Format: "PARTNER:123" ou "COURIER:456"

    @Indexed
    private TargetType entityType;

    @Indexed
    private Long entityId;

    /**
     * Note moyenne (1-5)
     */
    private Double averageRating;

    /**
     * Nombre total d'évaluations
     */
    @Builder.Default
    private Integer totalReviews = 0;

    /**
     * Distribution des notes
     * Format: {"1": 5, "2": 10, "3": 20, "4": 50, "5": 100}
     */
    private Map<String, Integer> ratingDistribution;

    /**
     * Nombre de notes 5 étoiles
     */
    @Builder.Default
    private Integer fiveStarCount = 0;

    /**
     * Nombre de notes 4 étoiles
     */
    @Builder.Default
    private Integer fourStarCount = 0;

    /**
     * Nombre de notes 3 étoiles
     */
    @Builder.Default
    private Integer threeStarCount = 0;

    /**
     * Nombre de notes 2 étoiles
     */
    @Builder.Default
    private Integer twoStarCount = 0;

    /**
     * Nombre de notes 1 étoile
     */
    @Builder.Default
    private Integer oneStarCount = 0;

    private LocalDateTime updatedAt;

    public enum TargetType {
        PARTNER,
        COURIER,
        PRODUCT
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Mettre à jour la note moyenne
     */
    public void updateRating(int newRating) {
        this.totalReviews++;
        
        // Mettre à jour le compteur par étoile
        switch (newRating) {
            case 5 -> this.fiveStarCount++;
            case 4 -> this.fourStarCount++;
            case 3 -> this.threeStarCount++;
            case 2 -> this.twoStarCount++;
            case 1 -> this.oneStarCount++;
        }
        
        // Calculer la nouvelle moyenne
        double totalScore = (this.averageRating != null ? this.averageRating * (this.totalReviews - 1) : 0) + newRating;
        this.averageRating = totalScore / this.totalReviews;
        
        // Mettre à jour la distribution
        this.ratingDistribution = Map.of(
            "1", this.oneStarCount,
            "2", this.twoStarCount,
            "3", this.threeStarCount,
            "4", this.fourStarCount,
            "5", this.fiveStarCount
        );
        
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Générer la clé unique pour l'entité
     */
    public static String generateEntityKey(TargetType type, Long entityId) {
        return type.name() + ":" + entityId;
    }
}
