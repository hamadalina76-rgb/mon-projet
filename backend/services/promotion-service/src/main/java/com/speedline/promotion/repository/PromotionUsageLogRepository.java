package com.speedline.promotion.repository;

import com.speedline.promotion.domain.PromotionUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionUsageLogRepository extends JpaRepository<PromotionUsageLog, Long> {

    List<PromotionUsageLog> findByPromotionId(Long promotionId);

    List<PromotionUsageLog> findByUserId(Long userId);

    Optional<PromotionUsageLog> findByPromotionIdAndUserIdAndOrderId(Long promotionId, Long userId, Long orderId);

    long countByPromotionIdAndStatus(Long promotionId, PromotionUsageLog.UsageStatus status);

    @Modifying
    @Query("UPDATE PromotionUsageLog l SET l.status = 'REVOKED', l.revokedAt = :now " +
           "WHERE l.promotionId = :promotionId AND l.userId = :userId AND l.orderId = :orderId " +
           "AND l.status = 'APPLIED'")
    int revoke(@Param("promotionId") Long promotionId,
               @Param("userId") Long userId,
               @Param("orderId") Long orderId,
               @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(l.discountAmount), 0) FROM PromotionUsageLog l " +
           "WHERE l.promotionId = :promotionId AND l.status = 'APPLIED'")
    java.math.BigDecimal sumDiscountByPromotion(@Param("promotionId") Long promotionId);

    @Query("SELECT COUNT(DISTINCT l.userId) FROM PromotionUsageLog l " +
           "WHERE l.promotionId = :promotionId AND l.status = 'APPLIED'")
    long countUniqueUsersByPromotionId(@Param("promotionId") Long promotionId);

    List<PromotionUsageLog> findByPromotionIdAndStatus(Long promotionId, PromotionUsageLog.UsageStatus status);

    /**
     * Sum all discounts (for global statistics)
     */
    @Query("SELECT COALESCE(SUM(l.discountAmount), 0) FROM PromotionUsageLog l WHERE l.status = 'APPLIED'")
    java.math.BigDecimal sumAllAppliedDiscount();

    /**
     * Count usages in a date range (for monthly KPI)
     */
    @Query("SELECT COUNT(l) FROM PromotionUsageLog l " +
           "WHERE l.status = 'APPLIED' AND l.createdAt >= :from AND l.createdAt < :to")
    long countAppliedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Daily usage counts (for line chart) — native query for DATE()
     */
    @Query(value = "SELECT DATE(created_at) AS day, COUNT(*) AS cnt " +
                   "FROM promotion_usage_log " +
                   "WHERE status = 'APPLIED' AND created_at >= :from " +
                   "GROUP BY DATE(created_at) ORDER BY day",
           nativeQuery = true)
    List<Object[]> countDailyUsageSince(@Param("from") LocalDateTime from);
}
