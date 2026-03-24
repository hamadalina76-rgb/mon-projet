package com.speedline.partner.service;

import com.speedline.partner.client.UserServiceClient;
import com.speedline.partner.domain.AuditLog;
import com.speedline.partner.domain.PartnerChangeLog;
import com.speedline.partner.domain.ProductHistoryBackup;
import com.speedline.partner.dto.AuditLogEntryDTO;
import com.speedline.partner.dto.PartnerChangeLogDTO;
import com.speedline.partner.dto.PartnerChangeLogFilterDTO;
import com.speedline.partner.dto.ProductHistoryBackupDTO;
import com.speedline.partner.repository.AuditLogRepository;
import com.speedline.partner.repository.PartnerChangeLogRepository;
import com.speedline.partner.repository.ProductRepository;
import com.speedline.partner.repository.ProductHistoryBackupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final PartnerChangeLogRepository partnerChangeLogRepository;
    private final ProductRepository productRepository;
    private final ProductHistoryBackupRepository productHistoryBackupRepository;
    private final UserServiceClient userServiceClient;
    private final Map<Long, String> adminNameCache = new ConcurrentHashMap<>();
    private final Set<Long> adminNameWarned = ConcurrentHashMap.newKeySet();

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

    @Async
    public void logWithChanges(
            Long adminId,
            String action,
            String entityType,
            Long entityId,
            String changesBefore,
            String changesAfter,
            String reason) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .adminId(adminId != null ? adminId : 0L)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .changesBefore(changesBefore)
                    .changesAfter(changesAfter)
                    .reason(reason)
                    .build();
            auditLogRepository.save(auditLog);
            log.info("✅ AuditLog(details): admin={} action={} entity={}:{}", adminId, action, entityType, entityId);
        } catch (Exception e) {
            log.error("❌ Erreur AuditLog(details): {}", e.getMessage());
        }
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
            String zoneIdsBefore,
            String zoneIdsAfter,
            String reason,
            Boolean productEditPermissionBefore,
            Boolean productEditPermissionAfter) {
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
                    .zoneIdsBefore(zoneIdsBefore)
                    .zoneIdsAfter(zoneIdsAfter)
                    .productEditPermissionBefore(productEditPermissionBefore)
                    .productEditPermissionAfter(productEditPermissionAfter)
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
        String cached = adminNameCache.get(adminId);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        try {
            // adminId dans partner_change_logs correspond au userId (ID auth JWT)
            Map<String, Object> admin = userServiceClient.getAdminByUserId(adminId);
            Object fullName = admin.get("fullName");
            if (fullName instanceof String s && !s.isBlank() && !isPlaceholderAdminName(s, adminId)) {
                adminNameCache.put(adminId, s);
                return s;
            }
        } catch (Exception ex) {
            if (adminNameWarned.add(adminId)) {
                log.info("Admin userId={} introuvable depuis user-service, fallback utilisé: {}", adminId, ex.getMessage());
            }
        }

        // Fallback 2: certaines sources peuvent stocker l'ID primaire admin
        // au lieu du userId JWT.
        try {
            Map<String, Object> adminById = userServiceClient.getAdminById(adminId);
            Object fullNameById = adminById.get("fullName");
            if (fullNameById instanceof String s && !s.isBlank()) {
                adminNameCache.put(adminId, s);
                return s;
            }
        } catch (Exception ex) {
            if (adminNameWarned.add(-adminId)) {
                log.info("Admin id={} introuvable via /admins/{id}, fallback final: {}", adminId, ex.getMessage());
            }
        }

        String fallback = "Admin";
        adminNameCache.put(adminId, fallback);
        return fallback;
    }

    private boolean isPlaceholderAdminName(String name, Long adminId) {
        String normalized = name == null ? "" : name.trim();
        return normalized.equalsIgnoreCase("Admin")
                || normalized.equalsIgnoreCase("Admin #" + adminId);
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
        String adminFullName = filters.getAdminFullName();

        if (adminId == null && adminFullName != null && !adminFullName.isBlank()) {
            List<Long> adminIds = resolveAdminUserIdsByName(adminFullName);
            if (adminIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return partnerChangeLogRepository
                    .searchLogsByAdminIds(partnerId, action, adminIds, dateFrom, dateTo, pageable)
                    .map(this::toPartnerChangeLogDTO);
        }

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

    public Page<AuditLogEntryDTO> getPartnerProductAuditLogs(Long partnerId, Pageable pageable) {
        List<Long> productIds = productRepository.findProductIdsByPartnerId(partnerId);
        if (productIds == null || productIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return auditLogRepository
                .findByEntityTypeAndEntityIdInOrderByTimestampDesc("PRODUCT", productIds, pageable)
                .map(this::toAuditLogEntryDTO);
    }

    public Page<ProductHistoryBackupDTO> getPartnerProductHistoryBackups(
            Long partnerId,
            String action,
            String actorType,
            Long actorId,
            String adminFullName,
            Long productId,
            String dateFrom,
            String dateTo,
            Pageable pageable) {
        LocalDateTime from = parseStartOfDay(dateFrom);
        LocalDateTime to = parseEndOfDay(dateTo);
        if (from == null) from = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        if (to == null) to = LocalDateTime.of(2999, 12, 31, 23, 59, 59);
        String actionFilter = (action != null && !action.isBlank()) ? action.trim() : null;
        String actorTypeFilter = (actorType != null && !actorType.isBlank()) ? actorType.trim() : null;
        try {
            if (actorId == null && adminFullName != null && !adminFullName.isBlank()) {
                if (actorTypeFilter != null && !"ADMIN".equalsIgnoreCase(actorTypeFilter)) {
                    return Page.empty(pageable);
                }
                List<Long> actorIds = resolveAdminUserIdsByName(adminFullName);
                if (actorIds.isEmpty()) {
                    return Page.empty(pageable);
                }
                return productHistoryBackupRepository
                        .searchByPartnerIdAndActorIds(partnerId, actionFilter, "ADMIN", actorIds, productId, from, to, pageable)
                        .map(this::toProductHistoryBackupDTO);
            }

            return productHistoryBackupRepository
                    .searchByPartnerId(partnerId, actionFilter, actorTypeFilter, actorId, productId, from, to, pageable)
                    .map(this::toProductHistoryBackupDTO);
        } catch (Exception ex) {
            log.warn("product_history_backups unavailable, fallback to audit_logs. partnerId={}, error={}",
                    partnerId, ex.getMessage());
            Pageable fallbackPageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp")
            );
            Page<AuditLogEntryDTO> fallback = getPartnerProductAuditLogs(partnerId, fallbackPageable);
            List<ProductHistoryBackupDTO> content = fallback.getContent().stream()
                    .map(a -> ProductHistoryBackupDTO.builder()
                            .id(a.getId())
                            .partnerId(partnerId)
                            .productId(null)
                            .action(a.getAction())
                            .actorType(a.getAdminId() != null ? "ADMIN" : "SYSTEM")
                            .actorId(a.getAdminId())
                            .adminName(a.getAdminName())
                            .changesBefore(a.getChangesBefore())
                            .changesAfter(a.getChangesAfter())
                            .reason(a.getReason())
                            .createdAt(a.getTimestamp())
                            .build())
                    .collect(Collectors.toList());
            return new PageImpl<>(content, pageable, fallback.getTotalElements());
        }
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
                .zoneIdsBefore(e.getZoneIdsBefore())
                .zoneIdsAfter(e.getZoneIdsAfter())
                .productEditPermissionBefore(e.getProductEditPermissionBefore())
                .productEditPermissionAfter(e.getProductEditPermissionAfter())
                .reason(e.getReason())
                .changedAt(e.getChangedAt())
                .build();
    }

    private AuditLogEntryDTO toAuditLogEntryDTO(AuditLog e) {
        return AuditLogEntryDTO.builder()
                .id(e.getId())
                .adminId(e.getAdminId())
                .adminName(resolveAdminName(e.getAdminId()))
                .action(e.getAction())
                .timestamp(e.getTimestamp())
                .changesBefore(e.getChangesBefore())
                .changesAfter(e.getChangesAfter())
                .reason(e.getReason())
                .status("SUCCESS")
                .build();
    }

    private ProductHistoryBackupDTO toProductHistoryBackupDTO(ProductHistoryBackup e) {
        return ProductHistoryBackupDTO.builder()
                .id(e.getId())
                .partnerId(e.getPartnerId())
                .productId(e.getProductId())
                .action(e.getAction())
                .actorType(e.getActorType())
                .actorId(e.getActorId())
                .adminName("ADMIN".equalsIgnoreCase(e.getActorType()) ? resolveAdminName(e.getActorId()) : null)
                .changesBefore(e.getChangesBefore())
                .changesAfter(e.getChangesAfter())
                .reason(e.getReason())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private List<Long> resolveAdminUserIdsByName(String adminFullName) {
        String query = adminFullName == null ? "" : adminFullName.trim();
        if (query.isBlank()) return List.of();
        try {
            Map<String, Object> response = userServiceClient.searchAdmins(query, 0, 100);
            Object contentObj = response.get("content");
            if (!(contentObj instanceof List<?> contentList)) return List.of();

            Set<Long> ids = new HashSet<>();
            for (Object item : contentList) {
                if (!(item instanceof Map<?, ?> row)) continue;
                Object userIdObj = row.get("userId");
                if (userIdObj instanceof Number n) {
                    ids.add(n.longValue());
                    continue;
                }
                if (userIdObj instanceof String s) {
                    try { ids.add(Long.parseLong(s)); } catch (Exception ignored) {}
                }
            }
            return ids.stream().toList();
        } catch (Exception ex) {
            log.warn("resolveAdminUserIdsByName failed for '{}': {}", adminFullName, ex.getMessage());
            return List.of();
        }
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
