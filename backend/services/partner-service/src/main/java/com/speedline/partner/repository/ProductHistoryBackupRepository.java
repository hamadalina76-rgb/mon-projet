package com.speedline.partner.repository;

import com.speedline.partner.domain.ProductHistoryBackup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductHistoryBackupRepository extends JpaRepository<ProductHistoryBackup, Long> {

    Page<ProductHistoryBackup> findByPartnerIdOrderByCreatedAtDesc(Long partnerId, Pageable pageable);

    List<ProductHistoryBackup> findByProductIdOrderByCreatedAtAsc(Long productId);

    @Query("SELECT p FROM ProductHistoryBackup p WHERE p.partnerId = :partnerId " +
           "AND (:action IS NULL OR p.action = :action) " +
           "AND (:actorType IS NULL OR p.actorType = :actorType) " +
           "AND (:actorId IS NULL OR p.actorId = :actorId) " +
           "AND (:productId IS NULL OR p.productId = :productId) " +
           "AND (p.createdAt >= :dateFrom) " +
           "AND (p.createdAt <= :dateTo) " +
           "ORDER BY p.createdAt DESC")
    Page<ProductHistoryBackup> searchByPartnerId(
            @Param("partnerId") Long partnerId,
            @Param("action") String action,
            @Param("actorType") String actorType,
            @Param("actorId") Long actorId,
            @Param("productId") Long productId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    @Query("SELECT p FROM ProductHistoryBackup p WHERE p.partnerId = :partnerId " +
           "AND (:action IS NULL OR p.action = :action) " +
           "AND (:actorType IS NULL OR p.actorType = :actorType) " +
           "AND p.actorId IN :actorIds " +
           "AND (:productId IS NULL OR p.productId = :productId) " +
           "AND (p.createdAt >= :dateFrom) " +
           "AND (p.createdAt <= :dateTo) " +
           "ORDER BY p.createdAt DESC")
    Page<ProductHistoryBackup> searchByPartnerIdAndActorIds(
            @Param("partnerId") Long partnerId,
            @Param("action") String action,
            @Param("actorType") String actorType,
            @Param("actorIds") List<Long> actorIds,
            @Param("productId") Long productId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    Optional<ProductHistoryBackup> findTopByProductIdAndActionInOrderByCreatedAtDesc(Long productId, List<String> actions);
}
