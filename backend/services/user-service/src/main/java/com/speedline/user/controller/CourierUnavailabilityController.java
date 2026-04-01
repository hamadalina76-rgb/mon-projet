package com.speedline.user.controller;

import com.speedline.user.domain.UnavailabilityValidationStatus;
import com.speedline.user.dto.CourierExceptionalScheduleDTO;
import com.speedline.user.service.CourierExceptionalScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/courier/unavailability")
@RequiredArgsConstructor
public class CourierUnavailabilityController {

    private final CourierExceptionalScheduleService service;

    @PostMapping
    public ResponseEntity<CourierExceptionalScheduleDTO> declareUnavailability(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CourierExceptionalScheduleDTO.CourierDeclarationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.declareUnavailability(userId, req));
    }

    @GetMapping("/me")
    public ResponseEntity<List<CourierExceptionalScheduleDTO>> getMyDeclarations(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) UnavailabilityValidationStatus validationStatus,
            @RequestParam(required = false, name = "state") String state) {
        UnavailabilityValidationStatus resolvedStatus =
                validationStatus != null ? validationStatus : mapStateAlias(state);
        return ResponseEntity.ok(service.getMyDeclarations(userId, resolvedStatus));
    }

    @PostMapping("/available")
    public ResponseEntity<CourierExceptionalScheduleDTO> markAsAvailable(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.markAsAvailable(userId));
    }

    private UnavailabilityValidationStatus mapStateAlias(String state) {
        if (state == null || state.isBlank()) return null;
        String normalized = state.trim().toUpperCase();
        return switch (normalized) {
            case "SUBMITTED", "SOUMIS", "EN_ATTENTE" -> UnavailabilityValidationStatus.PENDING_VALIDATION;
            case "IN_PROGRESS", "EN_COURS", "EN-COURS", "ACTIVE" -> UnavailabilityValidationStatus.APPROVED_ACTIVE;
            case "REJECTED", "REFUSED" -> UnavailabilityValidationStatus.REJECTED;
            case "RESOLVED", "DONE", "TERMINE" -> UnavailabilityValidationStatus.RESOLVED_AVAILABLE;
            default -> null;
        };
    }
}
