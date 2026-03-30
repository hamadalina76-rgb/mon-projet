package com.speedline.user.controller;

import com.speedline.user.dto.AppWorkingHoursDTO;
import com.speedline.user.service.AppWorkingHoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GET  /v1/admin/settings/working-hours        → horaires de la semaine
 * PUT  /v1/admin/settings/working-hours        → mise à jour complète
 * GET  /v1/admin/settings/working-hours/public → endpoint public (apps mobiles)
 */
@RestController
@RequestMapping("/v1/admin/settings/working-hours")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AppWorkingHoursController {

    private final AppWorkingHoursService service;

    @GetMapping
    public ResponseEntity<AppWorkingHoursDTO.WeeklyScheduleResponse> get() {
        return ResponseEntity.ok(service.getWeeklySchedule());
    }

    @PutMapping
    public ResponseEntity<AppWorkingHoursDTO.WeeklyScheduleResponse> update(
            @RequestBody AppWorkingHoursDTO.UpdateWeeklyScheduleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId) {
        return ResponseEntity.ok(service.updateWeeklySchedule(request, adminId));
    }

    /** Public endpoint — no auth required (used by customer_app / courier_app). */
    @GetMapping("/public")
    public ResponseEntity<AppWorkingHoursDTO.WeeklyScheduleResponse> getPublic() {
        return ResponseEntity.ok(service.getWeeklySchedule());
    }
}
