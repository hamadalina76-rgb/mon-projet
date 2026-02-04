package com.speedline.promotion.repository;

import com.speedline.promotion.domain.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour Promotion
 */
@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /**
     * Trouver une promotion par code
     */
    Optional<Promotion> findByCode(String code);

    /**
     * Vérifier si un code existe
     */
    boolean existsByCode(String code);

    /**
     * Trouver les promotions actives
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND (p.startDate IS NULL OR p.startDate <= :now) " +
           "AND (p.endDate IS NULL OR p.endDate >= :now) " +
           "AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);

    /**
     * Trouver les promotions actives avec pagination
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND (p.startDate IS NULL OR p.startDate <= :now) " +
           "AND (p.endDate IS NULL OR p.endDate >= :now) " +
           "AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)")
    Page<Promotion> findActivePromotionsPaginated(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Trouver les promotions expirées
     */
    @Query("SELECT p FROM Promotion p WHERE p.endDate IS NOT NULL AND p.endDate < :now")
    List<Promotion> findExpiredPromotions(@Param("now") LocalDateTime now);

    /**
     * Incrémenter le compteur d'utilisation
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.usageCount = p.usageCount + 1 WHERE p.id = :promotionId")
    int incrementUsageCount(@Param("promotionId") Long promotionId);

    /**
     * Désactiver les promotions expirées
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.isActive = false WHERE p.endDate IS NOT NULL AND p.endDate < :now")
    int deactivateExpiredPromotions(@Param("now") LocalDateTime now);

    /**
     * Compter les promotions actives
     */
    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.isActive = true " +
           "AND (p.startDate IS NULL OR p.startDate <= :now) " +
           "AND (p.endDate IS NULL OR p.endDate >= :now)")
    long countActivePromotions(@Param("now") LocalDateTime now);
}
