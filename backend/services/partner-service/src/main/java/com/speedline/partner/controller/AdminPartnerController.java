package com.speedline.partner.controller;

import com.speedline.partner.domain.PartnerStatus;
import com.speedline.partner.dto.PartnerDTO;
import com.speedline.partner.service.PartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
     * Approve a partner
     * POST /admin/partners/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approvePartner(@PathVariable Long id) {
        log.info("Admin: Approving partner id: {}", id);

        try {
            PartnerDTO partner = partnerService.approvePartner(id);
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
}
