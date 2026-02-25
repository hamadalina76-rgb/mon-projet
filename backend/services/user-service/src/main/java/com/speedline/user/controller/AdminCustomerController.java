package com.speedline.user.controller;

import com.speedline.user.domain.Customer.CustomerStatus;
import com.speedline.user.dto.CustomerDTO;
import com.speedline.user.service.AdminCustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * Contrôleur REST admin pour la gestion des clients (liste, filtres, actions, export, stats).
 * Base path après StripPrefix=1 : v1/admin/customers
 */
@RestController
@RequestMapping("v1/admin/customers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    /**
     * GET v1/admin/customers?page=0&size=20&sort=createdAt&sortDir=DESC&status=ACTIVE&search=...&dateFrom=...&dateTo=...
     * search : recherche sur nom, prénom, email, téléphone, ville
     */
    @GetMapping
    public ResponseEntity<Page<CustomerDTO>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        log.info("GET v1/admin/customers - page: {}, size: {}, status: {}, search: {}", page, size, status, search);
        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;
        return ResponseEntity.ok(adminCustomerService.searchCustomers(status, search, from, to, pageable));
    }

    /**
     * GET v1/admin/customers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomer(@PathVariable Long id) {
        log.info("GET v1/admin/customers/{}", id);
        return ResponseEntity.ok(adminCustomerService.getCustomerById(id));
    }

    /**
     * POST v1/admin/customers/{id}/block
     */
    @PostMapping("/{id}/block")
    public ResponseEntity<Void> blockCustomer(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminId,
            @RequestHeader(value = "X-Admin-Name", required = false) String adminName
    ) {
        log.info("POST v1/admin/customers/{}/block", id);
        adminCustomerService.blockCustomer(id, adminId, adminName);
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/customers/{id}/unblock
     */
    @PostMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockCustomer(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminId,
            @RequestHeader(value = "X-Admin-Name", required = false) String adminName
    ) {
        log.info("POST v1/admin/customers/{}/unblock", id);
        adminCustomerService.unblockCustomer(id, adminId, adminName);
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/customers/{id}/reset-password
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> sendResetPasswordEmail(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminId,
            @RequestHeader(value = "X-Admin-Name", required = false) String adminName
    ) {
        log.info("POST v1/admin/customers/{}/reset-password", id);
        adminCustomerService.sendResetPasswordEmail(id, adminId, adminName);
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/customers/{id}/send-notification
     * Body: { "subject": "...", "body": "..." }
     */
    @PostMapping("/{id}/send-notification")
    public ResponseEntity<Void> sendNotification(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload,
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminId,
            @RequestHeader(value = "X-Admin-Name", required = false) String adminName
    ) {
        log.info("POST v1/admin/customers/{}/send-notification", id);
        String subject = payload != null && payload.containsKey("subject") ? payload.get("subject") : "Notification SpeedLine";
        String body = payload != null && payload.containsKey("body") ? payload.get("body") : "";
        adminCustomerService.sendNotification(id, subject, body, adminId, adminName);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE v1/admin/customers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminId,
            @RequestHeader(value = "X-Admin-Name", required = false) String adminName
    ) {
        log.info("DELETE v1/admin/customers/{}", id);
        adminCustomerService.deleteCustomer(id, adminId, adminName);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET v1/admin/customers/export?format=csv|xlsx&status=...&search=...&dateFrom=...&dateTo=...
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCustomers(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        log.info("GET v1/admin/customers/export - format: {}", format);
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;
        byte[] bytes = adminCustomerService.exportCustomers(format, status, search, from, to);
        String contentType = "xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "text/csv";
        String filename = "clients_export." + ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format) ? "xlsx" : "csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    /**
     * GET v1/admin/customers/stats/new-by-month?year=2025
     */
    @GetMapping("/stats/new-by-month")
    public ResponseEntity<Map<String, Long>> getNewCustomersByMonth(
            @RequestParam(defaultValue = "2025") int year
    ) {
        log.info("GET v1/admin/customers/stats/new-by-month - year: {}", year);
        return ResponseEntity.ok(adminCustomerService.getNewCustomersByMonth(year));
    }
}
