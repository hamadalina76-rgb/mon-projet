package com.speedline.user.controller;

import com.speedline.user.client.LocationServiceClient;
import com.speedline.user.domain.Courier;
import com.speedline.user.domain.CourierType;
import com.speedline.user.domain.CourierStatus;
import com.speedline.user.dto.CourierChangeLogDTO;
import com.speedline.user.dto.CourierDTO;
import com.speedline.user.dto.CourierUpdateRequest;
import com.speedline.user.dto.ZoneCourierCountDTO;
import com.speedline.user.repository.CourierRepository;
import com.speedline.user.service.CourierChangeLogService;
import com.speedline.user.service.CourierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrôleur REST admin pour la gestion des livreurs (liste, filtres, approbation, rejet, suspension, activation).
 * Base path après StripPrefix=1 : v1/admin/couriers
 */
@RestController
@RequestMapping("v1/admin/couriers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminCourierController {

    private final CourierService         courierService;
    private final CourierChangeLogService changeLogService;
    private final LocationServiceClient locationServiceClient;
    private final CourierRepository courierRepository;

    /**
     * GET v1/admin/couriers?page=0&size=20&sort=createdAt&sortDir=DESC&status=...&search=...
     */
    @GetMapping
    public ResponseEntity<Page<CourierDTO>> getCouriers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) CourierStatus status,
            @RequestParam(required = false) CourierType courierType,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String search
    ) {
        log.info("GET v1/admin/couriers - page: {}, size: {}, status: {}, courierType: {}, zoneId: {}, search: {}", page, size, status, courierType, zoneId, search);
        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(courierService.searchCouriers(search, status, courierType, zoneId, pageable));
    }

    /**
     * GET v1/admin/couriers/pending?page=0&size=20
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<CourierDTO>> getPendingApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET v1/admin/couriers/pending - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(courierService.getCouriersAwaitingApproval(pageable));
    }

    /**
     * GET v1/admin/couriers/{id}
     */
    @GetMapping("/zones/courier-counts")
    public ResponseEntity<List<ZoneCourierCountDTO>> getZoneCourierCounts(
            @RequestParam(required = false) List<Long> zoneIds
    ) {
        Set<Long> filter = zoneIds != null ? new HashSet<>(zoneIds) : null;
        Map<Long, long[]> countsByZone = new HashMap<>();

        List<Courier> couriers = courierRepository.findAll();
        for (Courier courier : couriers) {
            if (courier.getAssignedZoneIds() == null || courier.getAssignedZoneIds().isEmpty()) {
                continue;
            }

            boolean isInternal = courier.getCourierType() == CourierType.INTERNAL;
            for (Long zoneId : courier.getAssignedZoneIds()) {
                if (zoneId == null) {
                    continue;
                }
                if (filter != null && !filter.contains(zoneId)) {
                    continue;
                }
                long[] counts = countsByZone.computeIfAbsent(zoneId, id -> new long[2]);
                if (isInternal) {
                    counts[0]++;
                } else {
                    counts[1]++;
                }
            }
        }

        List<ZoneCourierCountDTO> result = countsByZone.entrySet().stream()
                .map(entry -> ZoneCourierCountDTO.builder()
                        .zoneId(entry.getKey())
                        .internalCouriersCount(entry.getValue()[0])
                        .externalCouriersCount(entry.getValue()[1])
                        .build())
                .sorted(Comparator.comparing(ZoneCourierCountDTO::getZoneId))
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * GET v1/admin/couriers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourierDTO> getCourier(@PathVariable Long id) {
        log.info("GET v1/admin/couriers/{}", id);
        CourierDTO courier = courierService.getCourierById(id);
        // Backfill sync: ensures old assignments are propagated to location-service.
        syncZonesToLocationService(id, courier.getAssignedZoneIds());
        return ResponseEntity.ok(courier);
    }

    /**
     * POST v1/admin/couriers/{id}/approve
     * Body: { "courierType": "INTERNAL" | "EXTERNAL", "zoneIds": [1, 2, 3] }
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<CourierDTO> approveCourier(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> body) {
        String courierTypeStr = body != null ? String.valueOf(body.get("courierType")) : null;
        log.info("POST v1/admin/couriers/{}/approve - courierType: {}", id, courierTypeStr);
        com.speedline.user.domain.CourierType type;
        try {
            type = com.speedline.user.domain.CourierType.valueOf(courierTypeStr.toUpperCase());
        } catch (Exception e) {
            log.warn("Type de livreur invalide: {}", courierTypeStr);
            return ResponseEntity.badRequest().build();
        }
        List<Long> zoneIds = null;
        if (body != null && body.get("zoneIds") instanceof List<?> raw) {
            zoneIds = raw.stream()
                    .filter(o -> o instanceof Number)
                    .map(o -> ((Number) o).longValue())
                    .toList();
        }
        CourierDTO result = courierService.verifyDocuments(id, type, zoneIds);
        syncZonesToLocationService(id, result.getAssignedZoneIds());
        final List<Long> finalZoneIds = zoneIds;
        changeLogService.log("APPROVE", id,
                "PENDING_APPROVAL", "ACTIVE",
                null, courierTypeStr,
                null, finalZoneIds != null ? finalZoneIds.stream().map(String::valueOf).collect(Collectors.joining(",")) : null,
                null, null);
        return ResponseEntity.ok(result);
    }

    /**
     * POST v1/admin/couriers/{id}/reject
     * Body: { "reason": "..." }
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectCourier(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("POST v1/admin/couriers/{}/reject", id);
        String reason = body != null && body.containsKey("reason") ? (body.get("reason") != null ? body.get("reason") : "") : "";
        courierService.rejectDocuments(id, reason);
        changeLogService.log("REJECT", id, "PENDING_APPROVAL", "REJECTED", null, null, null, null, null, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/couriers/{id}/request-more-info
     * Body: { "message": "..." } (optional; avoids 405/EOF when client sends empty body)
     */
    @PostMapping("/{id}/request-more-info")
    public ResponseEntity<Void> requestMoreInfo(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("POST v1/admin/couriers/{}/request-more-info", id);
        String message = body != null && body.containsKey("message") ? body.get("message") : "";
        courierService.requestMoreInfo(id, message != null ? message : "");
        changeLogService.log("REQUEST_MORE_INFO", id, null, null, null, null, null, null, null, message);
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/couriers/{id}/deactivate (Block – désactivation définitive)
     * Body: { "reason": "..." } (optional)
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCourier(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("POST v1/admin/couriers/{}/deactivate", id);
        String reason = body != null && body.containsKey("reason") ? (body.get("reason") != null ? body.get("reason") : "") : "";
        CourierDTO before = courierService.getCourierById(id);
        courierService.deactivateCourier(id, reason);
        changeLogService.log("DEACTIVATE", id,
                before.getStatus() != null ? before.getStatus().name() : null, "DEACTIVATED",
                null, null, null, null, null, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * POST v1/admin/couriers/{id}/suspend
     * Body: { "reason": "..." } (optional)
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendCourier(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("POST v1/admin/couriers/{}/suspend", id);
        String reason = body != null && body.containsKey("reason") ? (body.get("reason") != null ? body.get("reason") : "") : "";
        CourierDTO before = courierService.getCourierById(id);
        courierService.suspendCourier(id, reason);
        changeLogService.log("SUSPEND", id,
                before.getStatus() != null ? before.getStatus().name() : null, "SUSPENDED",
                null, null, null, null, null, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT v1/admin/couriers/{id}/zones
     * Body: { "zoneIds": [1, 2, 3] }
     */
    @PutMapping("/{id}/zones")
    public ResponseEntity<CourierDTO> updateZones(@PathVariable Long id,
                                                  @RequestBody Map<String, Object> body) {
        log.info("PUT v1/admin/couriers/{}/zones", id);
        List<Long> zoneIds = null;
        if (body != null && body.get("zoneIds") instanceof List<?> raw) {
            zoneIds = raw.stream()
                    .filter(o -> o instanceof Number)
                    .map(o -> ((Number) o).longValue())
                    .toList();
        }
        CourierDTO before = courierService.getCourierById(id);
        String zonesBefore = before.getAssignedZoneIds() != null
                ? before.getAssignedZoneIds().stream().map(String::valueOf).collect(Collectors.joining(","))
                : null;
        CourierDTO result = courierService.updateAssignedZones(id, zoneIds != null ? zoneIds : List.of());
        syncZonesToLocationService(id, result.getAssignedZoneIds());
        String zonesAfter = zoneIds != null
                ? zoneIds.stream().map(String::valueOf).collect(Collectors.joining(","))
                : null;
        changeLogService.log("ASSIGN_ZONES", id, null, null, null, null, zonesBefore, zonesAfter, null, null);
        return ResponseEntity.ok(result);
    }

    private void syncZonesToLocationService(Long courierId, List<Long> zoneIds) {
        List<Long> safeZoneIds = zoneIds != null ? zoneIds : List.of();
        try {
            locationServiceClient.syncCourierZones(courierId, safeZoneIds);
        } catch (Exception e) {
            log.warn("Synchronisation zones->location-service échouée pour courierId={}: {}", courierId, e.getMessage());
        }
    }

    /**
     * PUT v1/admin/couriers/{id}
     * Body: CourierUpdateRequest (all fields optional)
     */
    @PutMapping("/{id}")
    public ResponseEntity<CourierDTO> updateCourier(@PathVariable Long id,
                                                    @RequestBody CourierUpdateRequest request) {
        log.info("PUT v1/admin/couriers/{}", id);
        CourierDTO before = courierService.getCourierById(id);
        CourierDTO result = courierService.updateCourier(id, request);

        // Log courier type change
        if (request.getCourierType() != null) {
            String typeBefore = before.getCourierType() != null ? before.getCourierType().name() : null;
            String typeAfter  = request.getCourierType().name();
            if (!typeAfter.equals(typeBefore)) {
                changeLogService.log("CHANGE_TYPE", id, null, null, typeBefore, typeAfter, null, null, null, null);
            }
        }

        // Log vehicle changes
        List<String> vehicleChanges = new ArrayList<>();
        if (request.getVehicleType() != null && !Objects.equals(
                request.getVehicleType().name(),
                before.getVehicleType() != null ? before.getVehicleType().name() : null)) {
            vehicleChanges.add("Type: " + (before.getVehicleType() != null ? before.getVehicleType() : "—") + " → " + request.getVehicleType());
        }
        if (request.getVehicleNumber() != null && !Objects.equals(request.getVehicleNumber(), before.getVehicleNumber())) {
            vehicleChanges.add("N°: " + (before.getVehicleNumber() != null ? before.getVehicleNumber() : "—") + " → " + request.getVehicleNumber());
        }
        if (request.getVehicleModel() != null && !Objects.equals(request.getVehicleModel(), before.getVehicleModel())) {
            vehicleChanges.add("Modèle: " + (before.getVehicleModel() != null ? before.getVehicleModel() : "—") + " → " + request.getVehicleModel());
        }
        if (request.getVehicleColor() != null && !Objects.equals(request.getVehicleColor(), before.getVehicleColor())) {
            vehicleChanges.add("Couleur: " + (before.getVehicleColor() != null ? before.getVehicleColor() : "—") + " → " + request.getVehicleColor());
        }
        if (!vehicleChanges.isEmpty()) {
            changeLogService.log("VEHICLE_UPDATED", id, null, null, null, null, null, null,
                    String.join(" | ", vehicleChanges), null);
        }

        // Log delivery zone/radius changes
        List<String> zoneChanges = new ArrayList<>();
        if (request.getPreferredDeliveryZone() != null && !Objects.equals(request.getPreferredDeliveryZone(), before.getPreferredDeliveryZone())) {
            zoneChanges.add("Zone: " + (before.getPreferredDeliveryZone() != null ? before.getPreferredDeliveryZone() : "—") + " → " + request.getPreferredDeliveryZone());
        }
        if (request.getMaxDeliveryRadius() != null && !Objects.equals(request.getMaxDeliveryRadius(), before.getMaxDeliveryRadius())) {
            zoneChanges.add("Rayon: " + (before.getMaxDeliveryRadius() != null ? before.getMaxDeliveryRadius() + "km" : "—") + " → " + request.getMaxDeliveryRadius() + "km");
        }
        if (!zoneChanges.isEmpty()) {
            changeLogService.log("DELIVERY_ZONE_UPDATED", id, null, null, null, null, null, null,
                    String.join(" | ", zoneChanges), null);
        }

        // Log banking info changes
        List<String> bankChanges = new ArrayList<>();
        if (request.getBankIban() != null && !Objects.equals(request.getBankIban(), before.getBankIban())) {
            bankChanges.add("IBAN modifié");
        }
        if (request.getBankAccountHolder() != null && !Objects.equals(request.getBankAccountHolder(), before.getBankAccountHolder())) {
            bankChanges.add("Titulaire: " + (before.getBankAccountHolder() != null ? before.getBankAccountHolder() : "—") + " → " + request.getBankAccountHolder());
        }
        if (!bankChanges.isEmpty()) {
            changeLogService.log("BANK_INFO_UPDATED", id, null, null, null, null, null, null,
                    String.join(" | ", bankChanges), null);
        }

        // Log documents changes
        List<String> docChanges = new ArrayList<>();
        if (request.getIdentityNumber() != null && !Objects.equals(request.getIdentityNumber(), before.getIdentityNumber())) {
            docChanges.add("N° identité modifié");
        }
        if (request.getIdentityDocumentFrontImage() != null && !Objects.equals(request.getIdentityDocumentFrontImage(), before.getIdentityDocumentFrontImage())) {
            docChanges.add("Recto identité modifié");
        }
        if (request.getIdentityDocumentBackImage() != null && !Objects.equals(request.getIdentityDocumentBackImage(), before.getIdentityDocumentBackImage())) {
            docChanges.add("Verso identité modifié");
        }
        if (request.getDrivingLicenseNumber() != null && !Objects.equals(request.getDrivingLicenseNumber(), before.getDrivingLicenseNumber())) {
            docChanges.add("N° permis modifié");
        }
        if (request.getDrivingLicenseImage() != null && !Objects.equals(request.getDrivingLicenseImage(), before.getDrivingLicenseImage())) {
            docChanges.add("Image permis modifié");
        }
        if (request.getDrivingLicenseExpiry() != null) {
            String expiryBefore = before.getDrivingLicenseExpiry() != null ? before.getDrivingLicenseExpiry().toLocalDate().toString() : "—";
            String expiryAfter = request.getDrivingLicenseExpiry().toString();
            if (!expiryAfter.equals(expiryBefore)) {
                docChanges.add("Expiration permis: " + expiryBefore + " → " + expiryAfter);
            }
        }
        if (!docChanges.isEmpty()) {
            changeLogService.log("DOCUMENTS_UPDATED", id, null, null, null, null, null, null,
                    String.join(" | ", docChanges), null);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * POST v1/admin/couriers/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateCourier(@PathVariable Long id) {
        log.info("POST v1/admin/couriers/{}/activate", id);
        CourierDTO before = courierService.getCourierById(id);
        courierService.reactivateCourier(id);
        changeLogService.log("ACTIVATE", id,
                before.getStatus() != null ? before.getStatus().name() : null, "ACTIVE",
                null, null, null, null, null, null);
        return ResponseEntity.ok().build();
    }

    /**
     * GET v1/admin/couriers/{id}/change-logs?page=0&size=20
     */
    @GetMapping("/{id}/change-logs")
    public ResponseEntity<Page<CourierChangeLogDTO>> getChangeLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET v1/admin/couriers/{}/change-logs - page: {}, size: {}", id, page, size);
        return ResponseEntity.ok(changeLogService.getChangeLogs(id, page, size));
    }
}
