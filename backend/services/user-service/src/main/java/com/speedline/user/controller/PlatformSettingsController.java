package com.speedline.user.controller;

import com.speedline.user.dto.PlatformSettingsDTO;
import com.speedline.user.service.PlatformSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour les paramètres généraux de la plateforme.
 *
 * Admin endpoints : GET / PUT  /v1/admin/settings/general
 * Public endpoint : GET        /v1/admin/settings/app-status
 */
@RestController
@RequestMapping("/v1/admin/settings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PlatformSettingsController {

    private final PlatformSettingsService service;

    @GetMapping("/general")
    public ResponseEntity<PlatformSettingsDTO.GeneralSettingsResponse> getGeneral() {
        return ResponseEntity.ok(service.getGeneralSettings());
    }

    @PutMapping("/general")
    public ResponseEntity<PlatformSettingsDTO.GeneralSettingsResponse> updateGeneral(
            @RequestBody PlatformSettingsDTO.GeneralSettingsRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId) {
        return ResponseEntity.ok(service.updateGeneralSettings(request, adminId));
    }

    /**
     * Endpoint public (pas d'auth) pour que les apps mobiles vérifient
     * si la plateforme est activée / en maintenance.
     */
    @GetMapping("/app-status")
    public ResponseEntity<PlatformSettingsDTO.AppStatusResponse> getAppStatus() {
        return ResponseEntity.ok(service.getAppStatus());
    }
}
