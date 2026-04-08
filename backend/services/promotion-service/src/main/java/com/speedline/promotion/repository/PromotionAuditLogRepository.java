package com.speedline.promotion.repository;

import com.speedline.promotion.domain.PromotionAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionAuditLogRepository extends JpaRepository<PromotionAuditLog, Long> {

    Page<PromotionAuditLog> findByPromotionIdOrderByCreatedAtDesc(Long promotionId, Pageable pageable);

    @Query(value = """
        SELECT * FROM promotion_audit_log l
        WHERE l.promotion_id = :promotionId
          AND (:action IS NULL OR l.action = CAST(:action AS VARCHAR))
          AND (:search IS NULL OR (
               LOWER(l.details) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))
               OR LOWER(l.performed_by) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))
          ))
        ORDER BY l.created_at DESC
        """, nativeQuery = true)
    Page<PromotionAuditLog> findFiltered(
            @Param("promotionId") Long promotionId,
            @Param("action") String action,
            @Param("search") String search,
            Pageable pageable);
}
