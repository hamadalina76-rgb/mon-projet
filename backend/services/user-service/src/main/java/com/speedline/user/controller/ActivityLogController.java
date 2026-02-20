package com.speedline.user.controller;

import com.speedline.user.dto.ActivityLogRequest;
import com.speedline.user.dto.ActivityLogResponse;
import com.speedline.user.service.ActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Contrôleur REST pour la gestion des logs d'activité
 */
@RestController
@RequestMapping("/v1/admin/activity-logs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ActivityLogController {
    
    private final ActivityLogService activityLogService;
    
    /**
     * Crée un nouveau log d'activité
     * POST /api/v1/admin/activity-logs
     */
    @PostMapping
    public ResponseEntity<ActivityLogResponse> createLog(@Valid @RequestBody ActivityLogRequest request) {
        log.info("POST /api/v1/admin/activity-logs - action: {} on {}", 
                 request.getAction(), request.getResource());
        ActivityLogResponse response = activityLogService.createLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Récupère tous les logs avec pagination et filtres
     * GET /api/v1/admin/activity-logs?page=0&size=25&adminId=1&resource=users&action=CREATE
     */
    @GetMapping
    public ResponseEntity<Page<ActivityLogResponse>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        log.info("GET /api/v1/admin/activity-logs - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ActivityLogResponse> logs;
        if (adminId != null || resource != null || action != null || startDate != null || endDate != null) {
            logs = activityLogService.searchLogs(adminId, resource, action, startDate, endDate, pageable);
        } else {
            logs = activityLogService.getAllLogs(pageable);
        }
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Récupère les logs d'un admin spécifique
     * GET /api/v1/admin/activity-logs/by-admin/{adminId}
     */
    @GetMapping("/by-admin/{adminId}")
    public ResponseEntity<Page<ActivityLogResponse>> getLogsByAdmin(
            @PathVariable Long adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        log.info("GET /api/v1/admin/activity-logs/by-admin/{}", adminId);
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLogResponse> logs = activityLogService.getLogsByAdmin(adminId, pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Récupère les logs par type de ressource
     * GET /api/v1/admin/activity-logs/by-resource/{resource}
     */
    @GetMapping("/by-resource/{resource}")
    public ResponseEntity<Page<ActivityLogResponse>> getLogsByResource(
            @PathVariable String resource,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        log.info("GET /api/v1/admin/activity-logs/by-resource/{}", resource);
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLogResponse> logs = activityLogService.getLogsByResource(resource, pageable);
        return ResponseEntity.ok(logs);
    }
}
