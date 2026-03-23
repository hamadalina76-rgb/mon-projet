package com.speedline.partner.repository;

import com.speedline.partner.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType,
            Long entityId
    );

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType,
            Long entityId,
            Pageable pageable
    );

    Page<AuditLog> findByEntityTypeAndEntityIdInOrderByTimestampDesc(
            String entityType,
            List<Long> entityIds,
            Pageable pageable
    );

    List<AuditLog> findByAdminIdOrderByTimestampDesc(Long adminId);

    Optional<AuditLog> findTopByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType,
            Long entityId
    );
}