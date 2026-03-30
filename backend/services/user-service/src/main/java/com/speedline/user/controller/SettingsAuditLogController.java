package com.speedline.user.controller;

import com.speedline.user.dto.SettingsAuditLogDTO;
import com.speedline.user.service.SettingsAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GET /v1/admin/settings/audit-logs
 *
 * Query params :
 *   action    — type exact (optionnel)
 *   adminName — recherche partielle insensible à la casse (optionnel)
 *   date      — YYYY-MM-DD — jour exact (optionnel)
 *   page      — numéro de page 0-basé (défaut 0)
 *   size      — taille de page (défaut 10)
 *
 * Retourne toujours une PagedLogResponse avec totalElements.
 */
@RestController
@RequestMapping("/v1/admin/settings/audit-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SettingsAuditLogController {

    private final SettingsAuditLogService service;

    @GetMapping
    public ResponseEntity<SettingsAuditLogDTO.PagedLogResponse> getLogs(
            @RequestParam(required = false)    String action,
            @RequestParam(required = false)    String adminName,
            @RequestParam(required = false)    String date,
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "10") int    size) {

        return ResponseEntity.ok(
                service.getFilteredLogs(action, adminName, date, page, size));
    }
}
