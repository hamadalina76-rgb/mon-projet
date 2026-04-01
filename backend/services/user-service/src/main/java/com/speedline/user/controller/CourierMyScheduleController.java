package com.speedline.user.controller;

import com.speedline.user.dto.CourierExceptionalScheduleDTO;
import com.speedline.user.dto.CourierScheduleDTO;
import com.speedline.user.repository.CourierRepository;
import com.speedline.user.service.CourierExceptionalScheduleService;
import com.speedline.user.service.CourierScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoints planning pour le livreur connecte (lecture seule).
 */
@RestController
@RequestMapping("v1/courier/schedule")
@RequiredArgsConstructor
public class CourierMyScheduleController {

    private final CourierRepository courierRepository;
    private final CourierScheduleService scheduleService;
    private final CourierExceptionalScheduleService exceptionalService;

    @GetMapping("/me")
    public ResponseEntity<CourierScheduleDTO.Response> getMySchedule(
            @RequestHeader("X-User-Id") Long userId
    ) {
        var courier = courierRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Livreur introuvable pour userId: " + userId));

        CourierScheduleDTO.Response response = scheduleService.getSchedule(courier.getId());
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * GET /v1/courier/schedule/me/week?from=YYYY-MM-DD
     *
     * Retourne le planning effectif (fixe + exceptions fusionnés) pour les 7 jours
     * à partir de [from]. Utilisé par l'app mobile pour afficher le calendrier
     * en temps réel avec les modifications admin.
     */
    @GetMapping("/me/week")
    public ResponseEntity<List<CourierScheduleDTO.EffectiveScheduleResponse>> getMyWeek(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from
    ) {
        var courier = courierRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Livreur introuvable pour userId: " + userId));

        List<CourierScheduleDTO.EffectiveScheduleResponse> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            week.add(scheduleService.getEffectiveSchedule(courier.getId(), from.plusDays(i)));
        }
        return ResponseEntity.ok(week);
    }

    /**
     * GET /v1/courier/schedule/me/exceptions?from=YYYY-MM-DD&to=YYYY-MM-DD
     *
     * Retourne les plannings exceptionnels (admin + propres déclarations) du livreur
     * connecté qui intersectent la plage [from, to].
     * Utilisé par l'app mobile pour afficher les exceptions sur le calendrier.
     */
    @GetMapping("/me/exceptions")
    public ResponseEntity<List<CourierExceptionalScheduleDTO>> getMyExceptions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        var courier = courierRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Livreur introuvable pour userId: " + userId));

        List<CourierExceptionalScheduleDTO> exceptions =
                exceptionalService.getByCourierInRange(courier.getId(), from, to);
        return ResponseEntity.ok(exceptions);
    }
}
