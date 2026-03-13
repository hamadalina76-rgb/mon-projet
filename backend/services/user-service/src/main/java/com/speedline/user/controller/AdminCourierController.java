package com.speedline.user.controller;

import com.speedline.user.domain.CourierStatus;
import com.speedline.user.dto.CourierDTO;
import com.speedline.user.service.CourierService;
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
 * Contrôleur REST admin pour la gestion des livreurs (liste, filtres, approbation, rejet, suspension, activation).
 * Base path après StripPrefix=1 : v1/admin/couriers
 */
@RestController
@RequestMapping("v1/admin/couriers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminCourierController {

    private final CourierService courierService;

    /**
     * GET v1/admin/couriers?page=0&size=20&sort=createdAt&sortDir=DESC&status=...&search=...
     */
    @GetMapping
    public ResponseEntity<Page<CourierDTO>> getCouriers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) CourierStatus status,
            @RequestParam(required = false) String search
    ) {
        log.info("GET v1/admin/couriers - page: {}, size: {}, status: {}, search: {}", page, size, status, search);
        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(courierService.searchCouriers(search, status, pageable));
    }

    /**
     * GET v1/admin/couriers/pending?page=0&size=20
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<CourierDTO>> getPendingApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET v1/admin/couriers/pending - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(courierService.getCouriersAwaitingApproval(pageable));
    }

    /**
     * GET v1/admin/couriers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourierDTO> getCourier(@PathVariable Long id) {
        log.info("GET v1/admin/couriers/{}", id);
        return ResponseEntity.ok(courierService.getCourierById(id));
    }

    /**
     * POST v1/admin/couriers/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<CourierDTO> approveCourier(@PathVariable Long id) {
        log.info("POST v1/admin/couriers/{}/approve", id);
        return ResponseEntity.ok(courierService.verifyDocuments(id));
    }

    /**
     * POST v1/admin/couriers/{id}/reject
     * Body: { "reason": "..." }
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectCourier(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("POST v1/admin/couriers/{}/reject", id);
        String reason = body != null && body.containsKey("reason") ? (body.get("reason") != null ? body.get("reason") : "") : "";
        courierService.rejectDocuments(id, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/couriers/{id}/request-more-info
     * Body: { "message": "..." } (optional; avoids 405/EOF when client sends empty body)
     */
    @PostMapping("/{id}/request-more-info")
    public ResponseEntity<Void> requestMoreInfo(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("POST v1/admin/couriers/{}/request-more-info", id);
        String message = body != null && body.containsKey("message") ? body.get("message") : "";
        courierService.requestMoreInfo(id, message != null ? message : "");
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/couriers/{id}/deactivate (Block – désactivation définitive)
     * Body: { "reason": "..." } (optional)
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCourier(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("POST v1/admin/couriers/{}/deactivate", id);
        String reason = body != null && body.containsKey("reason") ? (body.get("reason") != null ? body.get("reason") : "") : "";
        courierService.deactivateCourier(id, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/couriers/{id}/suspend
     * Body: { "reason": "..." } (optional)
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendCourier(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("POST v1/admin/couriers/{}/suspend", id);
        String reason = body != null && body.containsKey("reason") ? (body.get("reason") != null ? body.get("reason") : "") : "";
        courierService.suspendCourier(id, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/couriers/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateCourier(@PathVariable Long id) {
        log.info("POST v1/admin/couriers/{}/activate", id);
        courierService.reactivateCourier(id);
        return ResponseEntity.ok().build();
    }
}
