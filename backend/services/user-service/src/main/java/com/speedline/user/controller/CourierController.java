package com.speedline.user.controller;

import com.speedline.user.dto.*;
import com.speedline.user.domain.UnavailabilityValidationStatus;
import com.speedline.user.service.CourierExceptionalScheduleService;
import com.speedline.user.service.CourierScheduleService;
import com.speedline.user.service.impl.CourierServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des livreurs
 * 
 * Endpoints disponibles :
 * - GET    /couriers/{id}              - Récupérer un livreur par ID
 * - PUT    /couriers/{id}              - Mettre à jour le profil du livreur
 * - PUT    /couriers/{id}/availability - Changer la disponibilité
 * - GET    /couriers/{id}/statistics   - Récupérer les statistiques
 * - POST   /couriers/{id}/documents    - Uploader un document
 */
@RestController
@RequestMapping("/couriers")
@RequiredArgsConstructor
@Slf4j
public class CourierController implements ICourierController {

    private final CourierServiceImpl courierService;
    private final CourierScheduleService courierScheduleService;
    private final CourierExceptionalScheduleService courierExceptionalScheduleService;

    // ==================== CONSULTATION ====================

    /**
     * Récupère un livreur par son ID
     */
    @GetMapping("/{id}")
    @Override
    public ResponseEntity<CourierDTO> getCourierById(@PathVariable Long id) {
        log.info("GET /couriers/{} - Récupération du livreur", id);
        CourierDTO courier = courierService.getCourierById(id);
        return ResponseEntity.ok(courier);
    }

    // ==================== MISE À JOUR ====================

    /**
     * Met à jour le profil d'un livreur
     */
    @PutMapping("/{id}")
    @Override
    public ResponseEntity<CourierDTO> updateCourier(
            @PathVariable Long id,
            @Valid @RequestBody CourierUpdateRequest request) {
        log.info("PUT /couriers/{} - Mise à jour du profil", id);
        CourierDTO updated = courierService.updateCourier(id, request);
        return ResponseEntity.ok(updated);
    }

    // ==================== DISPONIBILITÉ ====================

    /**
     * Met à jour la disponibilité d'un livreur
     */
    @PutMapping("/{id}/availability")
    @Override
    public ResponseEntity<CourierDTO> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody CourierAvailabilityRequest request) {
        log.info("PUT /couriers/{}/availability - online={}, available={}", 
                id, request.getIsOnline(), request.getIsAvailable());
        CourierDTO updated = courierService.updateAvailability(id, request);
        return ResponseEntity.ok(updated);
    }

    // ==================== STATISTIQUES ====================

    /**
     * Récupère les statistiques d'un livreur
     */
    @GetMapping("/{id}/statistics")
    @Override
    public ResponseEntity<CourierStatisticsDTO> getCourierStatistics(@PathVariable Long id) {
        log.info("GET /couriers/{}/statistics - Récupération des statistiques", id);
        CourierStatisticsDTO stats = courierService.getCourierStatistics(id);
        return ResponseEntity.ok(stats);
    }

    // ==================== DOCUMENTS ====================

    /**
     * Upload un document pour un livreur
     */
    @PostMapping("/{id}/documents")
    @Override
    public ResponseEntity<CourierDTO> uploadDocument(
            @PathVariable Long id,
            @Valid @RequestBody CourierDocumentUploadRequest request) {
        log.info("POST /couriers/{}/documents - Upload document type: {}", id, request.getDocumentType());
        CourierDTO updated = courierService.uploadDocument(id, request);
        return ResponseEntity.ok(updated);
    }

    // ==================== ENDPOINT AUTHENTIFIÉ ====================

    /**
     * Récupère le profil du livreur connecté
     * Utilise le X-User-Id du header (ajouté par l'API Gateway depuis le JWT)
     */
    @GetMapping("/profile")
    public ResponseEntity<CourierDTO> getMyProfile(@RequestHeader(value = "X-User-Id", required = true) Long userId) {
        log.info("GET /couriers/profile - Récupération du profil pour userId: {}", userId);
        CourierDTO courier = courierService.getCourierByUserId(userId);
        return ResponseEntity.ok(courier);
    }

    /**
     * Retourne le planning fixe du livreur connecté (INTERNAL).
     * Endpoint aligné avec les routes courier déjà exposées par la gateway.
     */
    @GetMapping("/current_user/schedule")
    public ResponseEntity<CourierScheduleDTO.Response> getMySchedule(
            @RequestHeader(value = "X-User-Id", required = true) Long userId) {
        log.info("GET /couriers/current_user/schedule - Récupération planning pour userId: {}", userId);
        CourierDTO courier = courierService.getCourierByUserId(userId);
        CourierScheduleDTO.Response response = courierScheduleService.getSchedule(courier.getId());
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    /**
     * Déclare une indisponibilité pour le livreur connecté.
     */
    @PostMapping("/current_user/unavailability")
    public ResponseEntity<CourierExceptionalScheduleDTO> declareMyUnavailability(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @Valid @RequestBody CourierExceptionalScheduleDTO.CourierDeclarationRequest req) {
        log.info("POST /couriers/current_user/unavailability - Déclaration indisponibilité pour userId: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courierExceptionalScheduleService.declareUnavailability(userId, req));
    }

    /**
     * Liste des déclarations d'indisponibilité du livreur connecté.
     */
    @GetMapping("/current_user/unavailability")
    public ResponseEntity<List<CourierExceptionalScheduleDTO>> getMyUnavailabilityDeclarations(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @RequestParam(required = false) UnavailabilityValidationStatus validationStatus,
            @RequestParam(required = false, name = "state") String state) {
        log.info("GET /couriers/current_user/unavailability - Liste des déclarations pour userId: {}", userId);
        UnavailabilityValidationStatus resolvedStatus = validationStatus;
        if (resolvedStatus == null && state != null && !state.isBlank()) {
            String normalized = state.trim().toUpperCase();
            resolvedStatus = switch (normalized) {
                case "SUBMITTED", "SOUMIS", "EN_ATTENTE" -> UnavailabilityValidationStatus.PENDING_VALIDATION;
                case "IN_PROGRESS", "EN_COURS", "EN-COURS", "ACTIVE" -> UnavailabilityValidationStatus.APPROVED_ACTIVE;
                case "REJECTED", "REFUSED" -> UnavailabilityValidationStatus.REJECTED;
                case "RESOLVED", "DONE", "TERMINE" -> UnavailabilityValidationStatus.RESOLVED_AVAILABLE;
                default -> null;
            };
        }
        return ResponseEntity.ok(courierExceptionalScheduleService.getMyDeclarations(userId, resolvedStatus));
    }

    /**
     * Marque le livreur connecté comme disponible immédiatement.
     */
    @PostMapping("/current_user/unavailability/available")
    public ResponseEntity<CourierExceptionalScheduleDTO> markMyAvailabilityNow(
            @RequestHeader(value = "X-User-Id", required = true) Long userId) {
        log.info("POST /couriers/current_user/unavailability/available - Disponible immédiatement pour userId: {}", userId);
        return ResponseEntity.ok(courierExceptionalScheduleService.markAsAvailable(userId));
    }

    /**
     * Met à jour la documentation du livreur connecté (avec upload de fichiers)
     * Utilise le X-User-Id du header (ajouté par l'API Gateway depuis le JWT)
     */
    @PutMapping(value = "/current_user", consumes = "multipart/form-data")
    public ResponseEntity<CourierDTO> updateMyDocumentation(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String vehicleModel,
            @RequestParam(required = false) String vehicleColor,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String licenseNumber,
            @RequestParam(required = false) String licenseExpiryDate,
            @RequestParam(required = false) String accountHolder,
            @RequestParam(required = false) String accountNumber,
            @RequestPart(required = false) org.springframework.web.multipart.MultipartFile idCardFront,
            @RequestPart(required = false) org.springframework.web.multipart.MultipartFile idCardBack,
            @RequestPart(required = false) org.springframework.web.multipart.MultipartFile licenseFront,
            @RequestPart(required = false) org.springframework.web.multipart.MultipartFile licenseBack) {
        
        log.info("PUT /couriers/current_user - Mise à jour documentation pour userId: {}", userId);
        log.info("Données reçues - vehicleType: {}, vehicleModel: {}, plateNumber: {}", 
                 vehicleType, vehicleModel, plateNumber);
        log.info("Fichiers reçus - idFront: {}, idBack: {}, licenseFront: {}, licenseBack: {}", 
                 idCardFront != null ? idCardFront.getOriginalFilename() : "null",
                 idCardBack != null ? idCardBack.getOriginalFilename() : "null",
                 licenseFront != null ? licenseFront.getOriginalFilename() : "null",
                 licenseBack != null ? licenseBack.getOriginalFilename() : "null");
        
        try {
            CourierDTO updated = courierService.updateCourierDocumentationByUserId(
                    userId, vehicleType, vehicleModel, vehicleColor, plateNumber,
                    idNumber, licenseNumber, licenseExpiryDate,
                    accountHolder, accountNumber,
                    idCardFront, idCardBack, licenseFront, licenseBack);
            
            log.info("✅ Documentation mise à jour avec succès pour userId: {}", userId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour de la documentation pour userId {}: {}", 
                     userId, e.getMessage(), e);
            throw e;
        }
    }

    // ==================== ENDPOINTS INTERNES ====================

    /**
     * Créer un nouveau profil livreur (appel interne depuis auth-service)
     * Accessible uniquement par les autres services via Feign
     */
    @PostMapping("/internal")
    public ResponseEntity<CourierDTO> createCourierInternal(@Valid @RequestBody CourierCreateRequest request) {
        log.info("POST /couriers/internal - Création profil livreur pour userId: {}", request.getUserId());
        CourierDTO courier = courierService.createCourier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(courier);
    }
}
