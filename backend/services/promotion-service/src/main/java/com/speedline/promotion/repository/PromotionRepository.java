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
     * Décrémenter le compteur d'utilisation (revoke)
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.usageCount = GREATEST(p.usageCount - 1, 0) WHERE p.id = :promotionId")
    int decrementUsageCount(@Param("promotionId") Long promotionId);

    /**
     * Expirer les promotions dont la date de fin est passée (status=EXPIRED + isActive=false)
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.isActive = false, p.status = com.speedline.promotion.domain.PromotionStatus.EXPIRED WHERE p.endDate IS NOT NULL AND p.endDate < :now AND p.status <> com.speedline.promotion.domain.PromotionStatus.EXPIRED")
    int expirePromotions(@Param("now") LocalDateTime now);

    /**
     * Filtered paged search (admin list).
     * Native query to avoid Hibernate bytea casting issues with PostgreSQL.
     */
    @Query(value = "SELECT * FROM promotions p WHERE p.deleted = false AND " +
           "(CAST(:search AS text) IS NULL OR LOWER(p.code) LIKE LOWER('%' || CAST(:search AS text) || '%') " +
           "   OR LOWER(p.name) LIKE LOWER('%' || CAST(:search AS text) || '%')) " +
           "AND (CAST(:status AS text) IS NULL OR CAST(p.status AS text) = CAST(:status AS text)) " +
           "AND (CAST(:type AS text) IS NULL OR CAST(p.type AS text) = CAST(:type AS text)) " +
           "AND (CAST(:startFrom AS timestamp) IS NULL OR p.start_date >= CAST(:startFrom AS timestamp)) " +
           "AND (CAST(:startTo AS timestamp) IS NULL OR p.start_date <= CAST(:startTo AS timestamp)) " +
           "AND (CAST(:partnerId AS text) IS NULL OR p.applicable_partner_ids LIKE '%' || CAST(:partnerId AS text) || '%') " +
           "AND (CAST(:zoneId AS text) IS NULL OR p.applicable_zone_ids LIKE '%' || CAST(:zoneId AS text) || '%')",
           countQuery = "SELECT COUNT(*) FROM promotions p WHERE p.deleted = false AND " +
           "(CAST(:search AS text) IS NULL OR LOWER(p.code) LIKE LOWER('%' || CAST(:search AS text) || '%') " +
           "   OR LOWER(p.name) LIKE LOWER('%' || CAST(:search AS text) || '%')) " +
           "AND (CAST(:status AS text) IS NULL OR CAST(p.status AS text) = CAST(:status AS text)) " +
           "AND (CAST(:type AS text) IS NULL OR CAST(p.type AS text) = CAST(:type AS text)) " +
           "AND (CAST(:startFrom AS timestamp) IS NULL OR p.start_date >= CAST(:startFrom AS timestamp)) " +
           "AND (CAST(:startTo AS timestamp) IS NULL OR p.start_date <= CAST(:startTo AS timestamp)) " +
           "AND (CAST(:partnerId AS text) IS NULL OR p.applicable_partner_ids LIKE '%' || CAST(:partnerId AS text) || '%') " +
           "AND (CAST(:zoneId AS text) IS NULL OR p.applicable_zone_ids LIKE '%' || CAST(:zoneId AS text) || '%')",
           nativeQuery = true)
    Page<Promotion> findFiltered(@Param("search") String search,
                                 @Param("status") String status,
                                 @Param("type") String type,
                                 @Param("startFrom") LocalDateTime startFrom,
                                 @Param("startTo") LocalDateTime startTo,
                                 @Param("partnerId") String partnerId,
                                 @Param("zoneId") String zoneId,
                                 Pageable pageable);

    /**
     * Compter les promotions actives
     */
    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.isActive = true " +
           "AND (p.startDate IS NULL OR p.startDate <= :now) " +
           "AND (p.endDate IS NULL OR p.endDate >= :now)")
    long countActivePromotions(@Param("now") LocalDateTime now);

    /**
     * Count by status (for statistics)
     */
    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.status = :status")
    long countByStatus(@Param("status") com.speedline.promotion.domain.PromotionStatus status);

    /**
     * Sum all usage counts
     */
    @Query("SELECT COALESCE(SUM(p.usageCount), 0) FROM Promotion p")
    long sumAllUsageCount();

    /**
     * Find active promotions about to expire (for Redis cleanup + events).
     */
    @Query("SELECT p FROM Promotion p WHERE p.endDate IS NOT NULL AND p.endDate < :now " +
           "AND p.status <> com.speedline.promotion.domain.PromotionStatus.EXPIRED AND p.isActive = true")
    List<Promotion> findExpirablePromotions(@Param("now") LocalDateTime now);

    /**
     * Find SCHEDULED promotions whose start_date has arrived.
     */
    @Query("SELECT p FROM Promotion p WHERE p.status = com.speedline.promotion.domain.PromotionStatus.SCHEDULED " +
           "AND p.startDate IS NOT NULL AND p.startDate <= :now")
    List<Promotion> findScheduledToActivate(@Param("now") LocalDateTime now);
}
