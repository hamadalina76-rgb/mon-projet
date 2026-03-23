package com.speedline.partner.repository;

import com.speedline.partner.domain.PartnerChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PartnerChangeLogRepository extends JpaRepository<PartnerChangeLog, Long> {

    List<PartnerChangeLog> findByPartnerIdOrderByChangedAtDesc(Long partnerId);

    Page<PartnerChangeLog> findByPartnerIdOrderByChangedAtDesc(Long partnerId, Pageable pageable);

    @Query(
        value = "SELECT * FROM partner_change_logs l" +
                " WHERE l.partner_id = :partnerId" +
                " AND (CAST(:action AS VARCHAR) IS NULL OR l.action = :action)" +
                " AND (CAST(:adminId AS BIGINT) IS NULL OR l.admin_id = :adminId)" +
                " AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR l.changed_at >= CAST(:dateFrom AS TIMESTAMP))" +
                " AND (CAST(:dateTo AS TIMESTAMP) IS NULL OR l.changed_at <= CAST(:dateTo AS TIMESTAMP))" +
                " ORDER BY l.changed_at DESC",
        countQuery = "SELECT COUNT(*) FROM partner_change_logs l" +
                " WHERE l.partner_id = :partnerId" +
                " AND (CAST(:action AS VARCHAR) IS NULL OR l.action = :action)" +
                " AND (CAST(:adminId AS BIGINT) IS NULL OR l.admin_id = :adminId)" +
                " AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR l.changed_at >= CAST(:dateFrom AS TIMESTAMP))" +
                " AND (CAST(:dateTo AS TIMESTAMP) IS NULL OR l.changed_at <= CAST(:dateTo AS TIMESTAMP))",
        nativeQuery = true
    )
    Page<PartnerChangeLog> searchLogs(
            @Param("partnerId") Long partnerId,
            @Param("action")    String action,
            @Param("adminId")   Long adminId,
            @Param("dateFrom")  LocalDateTime dateFrom,
            @Param("dateTo")    LocalDateTime dateTo,
            Pageable pageable
    );

    @Query(
        value = "SELECT * FROM partner_change_logs l" +
                " WHERE l.partner_id = :partnerId" +
                " AND (CAST(:action AS VARCHAR) IS NULL OR l.action = :action)" +
                " AND l.admin_id IN (:adminIds)" +
                " AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR l.changed_at >= CAST(:dateFrom AS TIMESTAMP))" +
                " AND (CAST(:dateTo AS TIMESTAMP) IS NULL OR l.changed_at <= CAST(:dateTo AS TIMESTAMP))" +
                " ORDER BY l.changed_at DESC",
        countQuery = "SELECT COUNT(*) FROM partner_change_logs l" +
                " WHERE l.partner_id = :partnerId" +
                " AND (CAST(:action AS VARCHAR) IS NULL OR l.action = :action)" +
                " AND l.admin_id IN (:adminIds)" +
                " AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR l.changed_at >= CAST(:dateFrom AS TIMESTAMP))" +
                " AND (CAST(:dateTo AS TIMESTAMP) IS NULL OR l.changed_at <= CAST(:dateTo AS TIMESTAMP))",
        nativeQuery = true
    )
    Page<PartnerChangeLog> searchLogsByAdminIds(
            @Param("partnerId") Long partnerId,
            @Param("action") String action,
            @Param("adminIds") List<Long> adminIds,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );
}