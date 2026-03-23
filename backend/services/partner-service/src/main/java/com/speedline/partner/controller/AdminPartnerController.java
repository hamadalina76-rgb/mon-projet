package com.speedline.partner.controller;

import com.speedline.partner.domain.PartnerStatus;
import com.speedline.partner.domain.PartnerZone;
import com.speedline.partner.dto.*;
import com.speedline.partner.repository.PartnerZoneRepository;
import com.speedline.partner.client.LocationServiceClient;
import com.speedline.partner.service.AuditLogService;
import com.speedline.partner.service.PartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST Controller for Admin Partner Management
 *
 * Endpoints:
 * GET    /admin/partners          - List all partners (paginated, filterable)
 * GET    /admin/partners/pending  - List pending partners
 * GET    /admin/partners/stats    - Get partner statistics
 * GET    /admin/partners/{id}     - Get partner detail
 * POST   /admin/partners/{id}/approve  - Approve partner
 * POST   /admin/partners/{id}/reject   - Reject partner
 * POST   /admin/partners/{id}/suspend  - Suspend partner
 */
@RestController
@RequestMapping("/admin/partners")
@RequiredArgsConstructor
@Slf4j
public class AdminPartnerController {

    private final PartnerService partnerService;
    private final AuditLogService auditLogService;
    private final PartnerZoneRepository partnerZoneRepository;
    private final LocationServiceClient locationServiceClient;

    /**
     * List all partners with optional status and search (by name, brand, city).
     * Pagination is done on the backend.
     * GET /admin/partners?page=0&size=20&status=PENDING&search=pizza
     */
    @GetMapping
    public ResponseEntity<?> getAllPartners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        log.info("Admin: Getting partners list, page={}, size={}, status={}, search={}", page, size, status, search);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            PartnerStatus partnerStatus = (status != null && !status.isEmpty())
                    ? PartnerStatus.valueOf(status.toUpperCase())
                    : null;
            Page<PartnerDTO> partners = partnerService.getPartnersByStatusAndSearch(partnerStatus, search, pageable);

            return ResponseEntity.ok(partners);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid status: " + status));
        } catch (Exception e) {
            log.error("Failed to get partners list: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get partners: " + e.getMessage()));
        }
    }

    /**
     * List pending partners
     * GET /admin/partners/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingPartners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin: Getting pending partners");

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
            Page<PartnerDTO> partners = partnerService.getPartnersByStatus(PartnerStatus.PENDING, pageable);
            return ResponseEntity.ok(partners);
        } catch (Exception e) {
            log.error("Failed to get pending partners: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get pending partners: " + e.getMessage()));
        }
    }

    /**
     * Get partner statistics for admin dashboard
     * GET /admin/partners/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getPartnerStats() {
        log.info("Admin: Getting partner stats");

        try {
            Pageable singlePage = PageRequest.of(0, 1);
            long total = partnerService.getPartnersByStatus(null, singlePage).getTotalElements();
            long pending = partnerService.getPartnersByStatus(PartnerStatus.PENDING, singlePage).getTotalElements();
            long active = partnerService.getPartnersByStatus(PartnerStatus.ACTIVE, singlePage).getTotalElements();
            long rejected = partnerService.getPartnersByStatus(PartnerStatus.REJECTED, singlePage).getTotalElements();
            long suspended = partnerService.getPartnersByStatus(PartnerStatus.SUSPENDED, singlePage).getTotalElements();

            return ResponseEntity.ok(Map.of(
                    "total", total,
                    "pending", pending,
                    "active", active,
                    "rejected", rejected,
                    "suspended", suspended
            ));
        } catch (Exception e) {
            log.error("Failed to get partner stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get stats: " + e.getMessage()));
        }
    }

    /**
     * Get partner detail by ID
     * GET /admin/partners/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPartnerDetail(@PathVariable Long id) {
        log.info("Admin: Getting partner detail for id: {}", id);

        try {
            PartnerDTO partner = partnerService.getPartnerById(id);
            return ResponseEntity.ok(partner);
        } catch (Exception e) {
            log.error("Partner not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Approve a partner with commission setup
     * POST /admin/partners/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approvePartner(@PathVariable Long id, 
                                           @RequestBody(required = false) @Valid PartnerApprovalDTO approvalData) {
        log.info("Admin: Approving partner id: {} with commission data: {}", id, approvalData);

        try {
            PartnerDTO partner = Optional.ofNullable(approvalData)
                    .map(data -> partnerService.approvePartnerWithCommission(id, data))
                    // Fallback to simple approval for backward compatibility
                    .orElseGet(() -> partnerService.approvePartner(id));
            
            return ResponseEntity.ok(Map.of(
                    "message", "Partner approved successfully",
                    "partner", partner
            ));
        } catch (RuntimeException e) {
            log.error("Failed to approve partner {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject a partner
     * POST /admin/partners/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectPartner(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        log.info("Admin: Rejecting partner id: {} with reason: {}", id, reason);

        try {
            partnerService.rejectPartner(id, reason);
            return ResponseEntity.ok(Map.of(
                    "message", "Partner rejected successfully"
            ));
        } catch (RuntimeException e) {
            log.error("Failed to reject partner {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Suspend a partner
     * POST /admin/partners/{id}/suspend
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspendPartner(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        log.info("Admin: Suspending partner id: {} with reason: {}", id, reason);

        try {
            partnerService.suspendPartner(id, reason);
            return ResponseEntity.ok(Map.of(
                    "message", "Partner suspended successfully"
            ));
        } catch (RuntimeException e) {
            log.error("Failed to suspend partner {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Activate a partner (set status to ACTIVE)
     * POST /admin/partners/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activatePartner(@PathVariable Long id) {
        log.info("Admin: Activating partner id: {}", id);

        try {
            PartnerDTO partner = partnerService.activatePartner(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Partner activated successfully",
                    "partner", partner
            ));
        } catch (RuntimeException e) {
            log.error("Failed to activate partner {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Deactivate a partner (set status to INACTIVE, stop accepting orders)
     * POST /admin/partners/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivatePartner(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Deactivated by admin") : "Deactivated by admin";
        log.info("Admin: Deactivating partner id: {} with reason: {}", id, reason);

        try {
            PartnerDTO partner = partnerService.deactivatePartner(id, reason);
            return ResponseEntity.ok(Map.of(
                    "message", "Partner deactivated successfully",
                    "partner", partner
            ));
        } catch (RuntimeException e) {
            log.error("Failed to deactivate partner {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Request more information from a partner (set status to DOCUMENTS_MISSING)
     * POST /admin/partners/{id}/request-more-info
     */
    @PostMapping("/{id}/request-more-info")
    public ResponseEntity<?> requestMoreInfo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "Veuillez compléter les informations manquantes.");
        log.info("Admin: Requesting more info for partner id: {} with message: {}", id, message);

        try {
            PartnerDTO partner = partnerService.requestMoreInfo(id, message);
            return ResponseEntity.ok(Map.of(
                    "message", "Request sent successfully",
                    "partner", partner
            ));
        } catch (RuntimeException e) {
            log.error("Failed to request more info for partner {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update internal notes for a partner
     * POST /admin/partners/{id}/internal-notes
     */
    @PostMapping("/{id}/internal-notes")
    public ResponseEntity<?> updateInternalNotes(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String notes = body.getOrDefault("note", "");
        log.info("Admin: Updating internal notes for partner id: {}", id);

        try {
            PartnerDTO partner = partnerService.updateInternalNotes(id, notes);
            return ResponseEntity.ok(Map.of(
                    "message", "Internal notes updated successfully",
                    "partner", partner
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update internal notes for partner {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Historique des modifications du partenaire (paginé)
     * GET /admin/partners/{id}/change-logs?page=0&size=10
     */
    @GetMapping("/{id}/change-logs")
    public ResponseEntity<?> getPartnerChangeLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Admin: Getting change logs for partner id: {}, page={}, size={}", id, page, size);
        try {
            // La méthode du repository contient déjà un ordre "OrderByChangedAtDesc",
            // donc on évite d'ajouter un second tri potentiellement problématique.
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(auditLogService.getPartnerChangeLogs(id, pageable));
        } catch (Exception e) {
            log.error("Failed to get change logs for partner {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Modifier les informations de base d'un partenaire (Admin only)
     * PUT /admin/partners/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePartner(
            @PathVariable Long id,
            @RequestBody AdminPartnerUpdateDTO dto) {
        log.info("Admin: Updating partner id: {}", id);
        try {
            PartnerDTO updated = partnerService.adminUpdatePartner(id, dto);
            return ResponseEntity.ok(Map.of(
                    "message", "Partner updated successfully",
                    "partner", updated
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update partner {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * Historique des modifications filtré (filtres via POST body)
     * POST /admin/partners/{id}/change-logs/filter
     */
    // ==================== ZONES ====================

    /**
     * GET /admin/partners/{id}/zones
     * Retourne les zones assignées au partenaire, enrichies avec les données du location-service
     */
    @GetMapping("/{id}/zones")
    public ResponseEntity<?> getPartnerZones(@PathVariable Long id) {
        try {
            List<PartnerZone> assignments = partnerZoneRepository.findByPartnerId(id);
            if (assignments.isEmpty()) return ResponseEntity.ok(List.of());

            Map<Long, LocalDateTime> assignedAtMap = assignments.stream()
                    .collect(Collectors.toMap(PartnerZone::getZoneId, PartnerZone::getAssignedAt));

            try {
                // Fetch chaque zone par ID → retourne toutes les zones assignées
                // même si elles sont inactives (pas de filtrage par statut)
                List<ZoneInfoDTO> result = assignments.stream()
                        .map(a -> {
                            try {
                                ZoneInfoDTO z = locationServiceClient.getZoneById(a.getZoneId());
                                z.setAssignedAt(a.getAssignedAt());
                                return z;
                            } catch (Exception ex) {
                                log.warn("Zone {} not found in location-service: {}", a.getZoneId(), ex.getMessage());
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                log.warn("Location-service unavailable, returning zone IDs only: {}", e.getMessage());
                List<Map<String, Object>> fallback = assignments.stream()
                        .map(a -> Map.<String, Object>of("id", a.getZoneId(), "assignedAt", a.getAssignedAt()))
                        .collect(Collectors.toList());
                return ResponseEntity.ok(fallback);
            }
        } catch (Exception e) {
            log.error("Failed to get zones for partner {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /admin/partners/{id}/zones/assign
     * Body: { "zoneIds": [1, 2, 3] }
     * Remplace toutes les zones assignées par la nouvelle liste
     */
    @Transactional
    @PostMapping("/{id}/zones/assign")
    public ResponseEntity<?> assignZones(
            @PathVariable Long id,
            @RequestBody Map<String, List<Number>> body) {
        try {
            List<Long> zoneIds = body.getOrDefault("zoneIds", List.of()).stream()
                    .map(Number::longValue)
                    .collect(Collectors.toList());
            partnerZoneRepository.deleteAllByPartnerId(id);
            List<PartnerZone> newAssignments = zoneIds.stream()
                    .map(zoneId -> PartnerZone.builder()
                            .partnerId(id)
                            .zoneId(zoneId)
                            .assignedAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());
            partnerZoneRepository.saveAll(newAssignments);
            log.info("Admin: Assigned {} zones to partner {}", zoneIds.size(), id);
            return ResponseEntity.ok(Map.of(
                    "message", "Zones assigned successfully",
                    "assignedZoneIds", zoneIds
            ));
            // La requête native du repository possède déjà son ORDER BY l.changed_at DESC.
            // Supprimer le tri du Pageable évite que Spring ajoute un ORDER BY supplémentaire
            // qui peut référencer une colonne inexistante (ex: l.changedAt).
            Pageable pageable = PageRequest.of(filters.getPage(), filters.getSize());
            return ResponseEntity.ok(
                    auditLogService.getPartnerChangeLogsFiltered(id, filters, pageable)
            );
        } catch (Exception e) {
            log.error("Failed to assign zones to partner {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /admin/partners/{id}/zones/{zoneId}
     * Retire une zone spécifique d'un partenaire
     */
    @Transactional
    @DeleteMapping("/{id}/zones/{zoneId}")
    public ResponseEntity<?> removeZone(@PathVariable Long id, @PathVariable Long zoneId) {
        try {
            partnerZoneRepository.deleteByPartnerIdAndZoneId(id, zoneId);
            log.info("Admin: Removed zone {} from partner {}", zoneId, id);
            return ResponseEntity.ok(Map.of("message", "Zone removed successfully"));
        } catch (Exception e) {
            log.error("Failed to remove zone {} from partner {}: {}", zoneId, id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Historique produit (audit_logs sur entityType=PRODUCT pour ce partenaire)
     * GET /admin/partners/{id}/product-audit-logs?page=0&size=20
     */
    @GetMapping("/{id}/product-audit-logs")
    public ResponseEntity<?> getPartnerProductAuditLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            return ResponseEntity.ok(auditLogService.getPartnerProductAuditLogs(id, pageable));
        } catch (Exception e) {
            log.error("Failed to get product audit logs for partner {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Nouveau backup historique produit dédié
     * GET /admin/partners/{id}/product-history-backups?page=0&size=20
     */
    @GetMapping("/{id}/product-history-backups")
    public ResponseEntity<?> getPartnerProductHistoryBackups(
            @PathVariable Long id,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String adminFullName,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            return ResponseEntity.ok(auditLogService.getPartnerProductHistoryBackups(
                    id, action, actorType, actorId, adminFullName, productId, dateFrom, dateTo, pageable));
        } catch (Exception e) {
            log.error("Failed to get product history backups for partner {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
