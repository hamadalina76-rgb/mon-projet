package com.speedline.user.repository;

import com.speedline.user.domain.CourierChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface CourierChangeLogRepository extends JpaRepository<CourierChangeLog, Long> {

    Page<CourierChangeLog> findByCourierIdOrderByChangedAtDesc(Long courierId, Pageable pageable);

    @Query(value = """
            SELECT * FROM courier_change_logs
            WHERE courier_id = :courierId
              AND (:action IS NULL OR action = :action)
              AND (:dateFrom IS NULL OR changed_at >= :dateFrom)
              AND (:dateTo   IS NULL OR changed_at <= :dateTo)
            ORDER BY changed_at DESC
            """,
           countQuery = """
            SELECT COUNT(*) FROM courier_change_logs
            WHERE courier_id = :courierId
              AND (:action IS NULL OR action = :action)
              AND (:dateFrom IS NULL OR changed_at >= :dateFrom)
              AND (:dateTo   IS NULL OR changed_at <= :dateTo)
            """,
           nativeQuery = true)
    Page<CourierChangeLog> searchLogs(
            @Param("courierId") Long courierId,
            @Param("action")    String action,
            @Param("dateFrom")  LocalDateTime dateFrom,
            @Param("dateTo")    LocalDateTime dateTo,
            Pageable pageable);
}
