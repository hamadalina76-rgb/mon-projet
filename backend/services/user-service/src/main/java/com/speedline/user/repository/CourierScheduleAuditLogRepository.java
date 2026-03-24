package com.speedline.user.repository;

import com.speedline.user.domain.CourierScheduleAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CourierScheduleAuditLogRepository extends JpaRepository<CourierScheduleAuditLog, Long> {

    @Query("SELECT a FROM CourierScheduleAuditLog a " +
           "WHERE a.courierId = :courierId " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND a.createdAt >= :dateFrom " +
           "AND a.createdAt <= :dateTo")
    Page<CourierScheduleAuditLog> findWithFilters(
            @Param("courierId") Long courierId,
            @Param("action")    String action,
            @Param("dateFrom")  LocalDateTime dateFrom,
            @Param("dateTo")    LocalDateTime dateTo,
            Pageable pageable
    );
}
