package com.speedline.user.controller;

import com.speedline.user.dto.CourierScheduleAuditLogDTO;
import com.speedline.user.dto.CourierScheduleDTO;
import com.speedline.user.service.CourierScheduleAuditLogService;
import com.speedline.user.service.CourierScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Planning horaire par livreur (INTERNAL uniquement).
 * Base path après StripPrefix=1 : v1/admin/couriers/{courierId}/schedule
 */
@RestController
@RequestMapping("v1/admin/couriers/{courierId}/schedule")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CourierScheduleController {

    private final CourierScheduleService scheduleService;
    private final CourierScheduleAuditLogService auditLogService;

    /** Planning permanent actif */
    @GetMapping
    public ResponseEntity<CourierScheduleDTO.Response> getSchedule(@PathVariable Long courierId) {
        CourierScheduleDTO.Response r = scheduleService.getSchedule(courierId);
        return r != null ? ResponseEntity.ok(r) : ResponseEntity.noContent().build();
    }

    /** Tous les plannings (historique) */
    @GetMapping("/all")
    public ResponseEntity<List<CourierScheduleDTO.Response>> getAllSchedules(@PathVariable Long courierId) {
        return ResponseEntity.ok(scheduleService.getAllSchedules(courierId));
    }

    /** Créer / mettre à jour le planning (manuel ou depuis template) */
    @PostMapping
    public ResponseEntity<CourierScheduleDTO.Response> saveSchedule(@PathVariable Long courierId,
                                                                      @RequestBody CourierScheduleDTO.SaveRequest req) {
        log.info("POST schedule for courier {}", courierId);
        return ResponseEntity.ok(scheduleService.saveSchedule(courierId, req));
    }

    /** Appliquer un template existant */
    @PostMapping("/apply-template/{templateId}")
    public ResponseEntity<CourierScheduleDTO.Response> applyTemplate(@PathVariable Long courierId,
                                                                       @PathVariable Long templateId) {
        log.info("Apply template {} to courier {}", templateId, courierId);
        return ResponseEntity.ok(scheduleService.applyTemplate(courierId, templateId));
    }

    /** Copier les shifts d'un jour vers d'autres jours */
    @PostMapping("/copy-day")
    public ResponseEntity<CourierScheduleDTO.Response> copyDay(@PathVariable Long courierId,
                                                                @RequestBody CourierScheduleDTO.CopyDayRequest req) {
        log.info("Copy day {} → {} for courier {}", req.getSourceDay(), req.getTargetDays(), courierId);
        return ResponseEntity.ok(scheduleService.copyDay(courierId, req));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(@PathVariable Long courierId, @PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(courierId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    /** Journal d'audit des modifications de planning (paginé et filtrable) */
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<CourierScheduleAuditLogDTO.Response>> getAuditLogs(
            @PathVariable Long courierId,
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "10") int    size,
            @RequestParam(required = false)    String action,
            @RequestParam(required = false)    String dateFrom,
            @RequestParam(required = false)    String dateTo) {
        return ResponseEntity.ok(
                auditLogService.getAuditLogs(courierId, page, size, action, dateFrom, dateTo));
    }
}
