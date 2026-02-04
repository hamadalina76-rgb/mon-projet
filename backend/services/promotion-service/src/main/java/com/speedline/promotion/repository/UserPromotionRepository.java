package com.speedline.promotion.repository;

import com.speedline.promotion.domain.UserPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour UserPromotion
 */
@Repository
public interface UserPromotionRepository extends JpaRepository<UserPromotion, Long> {

    /**
     * Trouver l'utilisation d'une promotion par un utilisateur
     */
    Optional<UserPromotion> findByUserIdAndPromotionId(Long userId, Long promotionId);

    /**
     * Trouver toutes les promotions utilisées par un utilisateur
     */
    List<UserPromotion> findByUserId(Long userId);

    /**
     * Trouver tous les utilisateurs qui ont utilisé une promotion
     */
    List<UserPromotion> findByPromotionId(Long promotionId);

    /**
     * Compter le nombre d'utilisateurs ayant utilisé une promotion
     */
    long countByPromotionId(Long promotionId);

    /**
     * Vérifier si un utilisateur a déjà utilisé une promotion
     */
    boolean existsByUserIdAndPromotionId(Long userId, Long promotionId);

    /**
     * Incrémenter le compteur d'utilisation
     */
    @Modifying
    @Query("UPDATE UserPromotion up SET up.usageCount = up.usageCount + 1, " +
           "up.lastUsedAt = CURRENT_TIMESTAMP " +
           "WHERE up.id = :id")
    int incrementUsage(@Param("id") Long id);

    /**
     * Créer ou mettre à jour l'utilisation
     * Si existe, incrémente; sinon crée
     */
    @Modifying
    @Query(value = "INSERT INTO user_promotions (user_id, promotion_id, usage_count, first_used_at, last_used_at, created_at) " +
           "VALUES (:userId, :promotionId, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
           "ON CONFLICT (user_id, promotion_id) DO UPDATE SET " +
           "usage_count = user_promotions.usage_count + 1, " +
           "last_used_at = CURRENT_TIMESTAMP", nativeQuery = true)
    int upsertUsage(@Param("userId") Long userId, @Param("promotionId") Long promotionId);
}
