package com.speedline.user.controller;

import com.speedline.user.dto.CourierExceptionalScheduleDTO;
import com.speedline.user.service.CourierExceptionalScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * API REST pour les plannings exceptionnels (ponctuels) des livreurs.
 *
 * Base URL : /v1/admin/exceptional-schedules
 * (Le gateway strips /api, le service reçoit /v1/admin/…)
 */
@RestController
@RequestMapping("v1/admin/exceptional-schedules")
@RequiredArgsConstructor
public class CourierExceptionalScheduleController {

    private final CourierExceptionalScheduleService service;

    // ── Liste paginée ───────────────────────────────────────────────────────

    /**
     * GET /v1/admin/exceptional-schedules
     *
     * Query params : page, size, search, exceptionType, dateFrom, dateTo
     */
    @GetMapping
    public ResponseEntity<Page<CourierExceptionalScheduleDTO>> getAll(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String exceptionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false)    Long   courierId) {

        return ResponseEntity.ok(service.getAll(page, size, search, exceptionType, dateFrom, dateTo, courierId));
    }

    // ── Par livreur + plage (calendrier) ────────────────────────────────────

    /**
     * GET /v1/admin/exceptional-schedules/courier/{courierId}?from=&to=
     *
     * Retourne les exceptions actives qui intersectent la plage [from, to].
     * Utilisé pour rendre les badges EXCEPTIONNEL dans le calendrier.
     */
    @GetMapping("/courier/{courierId}")
    public ResponseEntity<List<CourierExceptionalScheduleDTO>> getByCourier(
            @PathVariable Long courierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(service.getByCourierInRange(courierId, from, to));
    }

    // ── Vérification de chevauchement (sans créer) ──────────────────────────

    /**
     * GET /v1/admin/exceptional-schedules/check-overlap
     *
     * Query params : courierId, startDate, endDate, excludeId (optionnel)
     */
    @GetMapping("/check-overlap")
    public ResponseEntity<CourierExceptionalScheduleDTO.OverlapResult> checkOverlap(
            @RequestParam Long courierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long excludeId) {

        return ResponseEntity.ok(service.checkOverlap(courierId, startDate, endDate, excludeId));
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    /**
     * POST /v1/admin/exceptional-schedules
     */
    @PostMapping
    public ResponseEntity<CourierExceptionalScheduleDTO> create(
            @Valid @RequestBody CourierExceptionalScheduleDTO.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    /**
     * PUT /v1/admin/exceptional-schedules/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CourierExceptionalScheduleDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CourierExceptionalScheduleDTO.CreateRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

}
