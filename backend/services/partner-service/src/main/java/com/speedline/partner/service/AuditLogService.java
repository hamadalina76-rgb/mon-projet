package com.speedline.partner.service;

import com.speedline.partner.client.UserServiceClient;
import com.speedline.partner.domain.AuditLog;
import com.speedline.partner.domain.PartnerChangeLog;
import com.speedline.partner.dto.AuditLogEntryDTO;
import com.speedline.partner.dto.PartnerChangeLogDTO;
import com.speedline.partner.dto.PartnerChangeLogFilterDTO;
import com.speedline.partner.repository.AuditLogRepository;
import com.speedline.partner.repository.PartnerChangeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final PartnerChangeLogRepository partnerChangeLogRepository;
    private final UserServiceClient userServiceClient;

    // ── Audit log générique ──────────────────────────────────────────────────

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
            log.info("✅ AuditLog: admin={} action={} entity={}:{}", adminId, action, entityType, entityId);
        } catch (Exception e) {
            log.error("❌ Erreur AuditLog: {}", e.getMessage());
        }
    }

    @Async
    public void log(Long adminId, String action, String entityType, Long entityId) {
        log(adminId, action, entityType, entityId, null);
    }

    // ── Logs spécifiques partenaire ──────────────────────────────────────────

    /**
     * Enregistre une modification dans partner_change_logs (table dédiée).
     * Appelé de manière asynchrone pour ne pas bloquer la réponse.
     */
    @Async
    public void logPartnerModification(
            Long adminId,
            String action,
            Long partnerId,
            String statusBefore,
            String statusAfter,
            String commissionTypeBefore,
            String commissionTypeAfter,
            BigDecimal commissionRateBefore,
            BigDecimal commissionRateAfter,
            String categoryIdsBefore,
            String categoryIdsAfter,
            String reason) {
        try {
            PartnerChangeLog entry = PartnerChangeLog.builder()
                    .adminId(adminId)
                    .action(action)
                    .partnerId(partnerId)
                    .statusBefore(statusBefore)
                    .statusAfter(statusAfter)
                    .commissionTypeBefore(commissionTypeBefore)
                    .commissionTypeAfter(commissionTypeAfter)
                    .commissionRateBefore(commissionRateBefore)
                    .commissionRateAfter(commissionRateAfter)
                    .categoryIdsBefore(categoryIdsBefore)
                    .categoryIdsAfter(categoryIdsAfter)
                    .reason(reason)
                    .build();
            partnerChangeLogRepository.save(entry);
            log.info("✅ PartnerChangeLog: admin={} action={} partnerId={}", adminId, action, partnerId);
        } catch (Exception e) {
            log.error("❌ Erreur PartnerChangeLog: {}", e.getMessage());
        }
    }

    /**
     * Récupère l'historique des modifications d'un partenaire (paginé, tri desc par date).
     * Le fullName de l'admin est résolu via un appel Feign à user-service.
     */
    public Page<PartnerChangeLogDTO> getPartnerChangeLogs(Long partnerId, Pageable pageable) {
        return partnerChangeLogRepository
                .findByPartnerIdOrderByChangedAtDesc(partnerId, pageable)
            .map(this::toPartnerChangeLogDTO);
    }

    /**
     * Résout le fullName d'un admin via user-service.
     * Retourne "Admin #id" en cas d'erreur (circuit breaker / service indisponible).
     */
    private String resolveAdminName(Long adminId) {
        if (adminId == null) return "Système";
        try {
            // adminId dans partner_change_logs correspond au userId (ID auth JWT)
            Map<String, Object> admin = userServiceClient.getAdminByUserId(adminId);
            Object fullName = admin.get("fullName");
            if (fullName instanceof String s && !s.isBlank()) return s;
        } catch (Exception ex) {
            log.warn("⚠️ Impossible de récupérer le nom de l'admin userId={} depuis user-service: {}", adminId, ex.getMessage());
        }
        return "Admin #" + adminId;
    }

    // ── Compatibilité ancienne méthode (audit_logs) ──────────────────────────

    @Async
    public void logPartnerChange(Long adminId, String action, Long partnerId,
                                  String changesBefore, String changesAfter, String reason) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .adminId(adminId)
                    .action(action)
                    .entityType("PARTNER")
                    .entityId(partnerId)
                    .changesBefore(changesBefore)
                    .changesAfter(changesAfter)
                    .reason(reason)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("❌ Erreur logPartnerChange: {}", e.getMessage());
        }
    }
    public Page<PartnerChangeLogDTO> getPartnerChangeLogsFiltered(
            Long partnerId,
            PartnerChangeLogFilterDTO filters,
            Pageable pageable) {

        LocalDateTime dateFrom = parseStartOfDay(filters.getDateFrom());
        LocalDateTime dateTo   = parseEndOfDay(filters.getDateTo());

        String action  = (filters.getAction()  != null && !filters.getAction().isBlank())  ? filters.getAction()  : null;
        Long   adminId = filters.getAdminId();

        return partnerChangeLogRepository
                .searchLogs(partnerId, action, adminId, dateFrom, dateTo, pageable)
            .map(this::toPartnerChangeLogDTO);
    }
    public List<AuditLogEntryDTO> getPartnerAuditLogs(Long partnerId) {
        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc("PARTNER", partnerId)
                .stream()
                .map(this::toAuditLogEntryDTO)
                .collect(Collectors.toList());
    }

    // ── Helpers internes ────────────────────────────────────────────────────

    private PartnerChangeLogDTO toPartnerChangeLogDTO(PartnerChangeLog e) {
        return PartnerChangeLogDTO.builder()
                .id(e.getId())
                .partnerId(e.getPartnerId())
                .adminId(e.getAdminId())
                .adminName(resolveAdminName(e.getAdminId()))
                .action(e.getAction())
                .statusBefore(e.getStatusBefore())
                .statusAfter(e.getStatusAfter())
                .commissionTypeBefore(e.getCommissionTypeBefore())
                .commissionTypeAfter(e.getCommissionTypeAfter())
                .commissionRateBefore(e.getCommissionRateBefore())
                .commissionRateAfter(e.getCommissionRateAfter())
                .categoryIdsBefore(e.getCategoryIdsBefore())
                .categoryIdsAfter(e.getCategoryIdsAfter())
                .reason(e.getReason())
                .changedAt(e.getChangedAt())
                .build();
    }

    private AuditLogEntryDTO toAuditLogEntryDTO(AuditLog e) {
        return AuditLogEntryDTO.builder()
                .id(e.getId())
                .adminId(e.getAdminId())
                .adminName("Admin #" + e.getAdminId())
                .action(e.getAction())
                .timestamp(e.getTimestamp())
                .changesBefore(e.getChangesBefore())
                .changesAfter(e.getChangesAfter())
                .reason(e.getReason())
                .status("SUCCESS")
                .build();
    }

    private LocalDateTime parseStartOfDay(String date) {
        return parseDate(date, "T00:00:00", "dateFrom");
    }

    private LocalDateTime parseEndOfDay(String date) {
        return parseDate(date, "T23:59:59", "dateTo");
    }

    private LocalDateTime parseDate(String date, String timeSuffix, String label) {
        return Optional.ofNullable(date)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s + timeSuffix)
                .map(s -> {
                    try {
                        return LocalDateTime.parse(s);
                    } catch (Exception e) {
                        log.warn("⚠️ {} invalide: {}", label, date);
                        return null;
                    }
                })
                .orElse(null);
    }

}
