package com.speedline.user.repository;

import com.speedline.user.domain.SettingsAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingsAuditLogRepository
        extends JpaRepository<SettingsAuditLog, Long>,
                JpaSpecificationExecutor<SettingsAuditLog> {

    /** Les 50 entrées les plus récentes, toutes actions confondues. */
    List<SettingsAuditLog> findTop50ByOrderByCreatedAtDesc();
}
