package com.speedline.partner.service;

import com.speedline.partner.domain.AuditLog;
import com.speedline.partner.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Enregistrer une action d'audit (async pour ne pas bloquer la réponse)
     */
    @Async
    public void log(Long adminId, String action, String entityType, Long entityId, String reason) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .adminId(adminId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .reason(reason)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("✅ AuditLog enregistré: admin={} action={} entity={}:{}",
                    adminId, action, entityType, entityId);
        } catch (Exception e) {
            log.error("❌ Erreur enregistrement AuditLog: {}", e.getMessage());
        }
    }

    /**
     * Surcharge sans reason
     */
    @Async
    public void log(Long adminId, String action, String entityType, Long entityId) {
        log(adminId, action, entityType, entityId, null);
    }
}