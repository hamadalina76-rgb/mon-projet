package com.speedline.location.controller;

import com.speedline.location.domain.Zone;
import com.speedline.location.dto.*;
import com.speedline.location.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller pour Zone
 */
@RestController
@RequestMapping("/zones")
@RequiredArgsConstructor
@Slf4j
public class ZoneController {

    private final ZoneService zoneService;

    /**
     * Liste paginée des zones (optionnel: filtre par nom/ville et par statut actif/inactif)
     */
    @GetMapping
    public ResponseEntity<Page<ZoneDTO>> getAllZones(
            @PageableDefault(size = 20, sort = {"isActive", "updatedAt"}, direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive) {
        log.debug("Récupération des zones paginées, search={}, isActive={}", search, isActive);
        return ResponseEntity.ok(zoneService.getAllZones(pageable, search, isActive));
    }

    /**
     * Liste des zones actives
     */
    @GetMapping("/active")
    public ResponseEntity<List<ZoneDTO>> getActiveZones() {
        log.debug("Récupération de toutes les zones actives");
        return ResponseEntity.ok(zoneService.getActiveZones());
    }

    /**
     * Zones par type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ZoneDTO>> getZonesByType(
            @PathVariable Zone.ZoneType type) {
        log.debug("Récupération des zones par type: {}", type);
        return ResponseEntity.ok(zoneService.getZonesByType(type));
    }

    /**
     * Détails d'une zone
     */
    @GetMapping("/{id}")
    public ResponseEntity<ZoneDTO> getZoneById(@PathVariable Long id) {
        log.debug("Récupération de la zone ID: {}", id);
        return ResponseEntity.ok(zoneService.getZoneById(id));
    }

    /**
     * Créer une nouvelle zone
     */
    @PostMapping
    public ResponseEntity<ZoneDTO> createZone(@Valid @RequestBody ZoneCreateRequest request) {
        log.info("Création d'une nouvelle zone: {}", request.getName());
        ZoneDTO zone = zoneService.createZone(
                request.getName(),
                request.getDescription(),
                request.getCity(),
                request.getType(),
                request.getBoundaryJson(),
                request.getDeliveryFee(),
                request.getServiceFee(),
                request.getMinDeliveryTime(),
                request.getMaxDeliveryTime(),
                request.getRadiusKm(),
                request.getMinActiveInternalCouriers(),
                request.getMaxSimultaneousOrders(),
                request.getInterZoneExtensionRadiusKm(),
                request.getMaxInterZoneReassignmentDelayMinutes(),
                request.getInternalCourierAssignments()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(zone);
    }

    /**
     * Mettre à jour une zone
     */
    @PutMapping("/{id}")
    public ResponseEntity<ZoneDTO> updateZone(
            @PathVariable Long id,
            @Valid @RequestBody ZoneUpdateRequest request) {
        log.info("Mise à jour de la zone ID: {}", id);
        ZoneDTO zone = zoneService.updateZone(
                id,
                request.getName(),
                request.getDescription(),
                request.getDeliveryFee(),
                request.getServiceFee(),
                request.getBoundaryJson(),
                request.getRadiusKm(),
                request.getMinActiveInternalCouriers(),
                request.getMaxSimultaneousOrders(),
                request.getInterZoneExtensionRadiusKm(),
                request.getMaxInterZoneReassignmentDelayMinutes(),
                request.getInternalCourierAssignments()
        );
        return ResponseEntity.ok(zone);
    }

    /**
     * Activer/Désactiver une zone.
     * PUT et PATCH sont supportés : les clients Feign (HTTP par défaut) utilisent souvent PUT car PATCH peut être indisponible.
     */
    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ZoneDTO> setActiveStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        Boolean isActive = request.get("isActive");
        if (isActive == null) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Changement du statut de la zone ID: {} -> {}", id, isActive);
        return ResponseEntity.ok(zoneService.setActiveStatus(id, isActive));
    }

    /**
     * Supprimer une zone
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        log.info("Suppression de la zone ID: {}", id);
        zoneService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Vérifier si un point est dans une zone
     */
    @GetMapping("/{id}/contains")
    public ResponseEntity<Map<String, Boolean>> isPointInZone(
            @PathVariable Long id,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        log.debug("Vérification si le point est dans la zone ID: {}", id);
        boolean contains = zoneService.isPointInZone(id, latitude, longitude);
        return ResponseEntity.ok(Map.of("contains", contains));
    }

    /**
     * Vérifier via PostGIS si un point est dans une zone.
     * Si zoneId est fourni, teste uniquement cette zone.
     * Sinon, cherche la première zone contenant le point.
     */
    @PostMapping("/check")
    public ResponseEntity<com.speedline.location.dto.ZoneCheckResponse> checkPointInAnyZone(
            @RequestBody com.speedline.location.dto.ZoneCheckRequest request
    ) {
        boolean inZone;
        Long zoneId = null;

        if (request.zoneId() != null) {
            inZone = zoneService.isPointInZone(
                    request.zoneId(),
                    BigDecimal.valueOf(request.lat()),
                    BigDecimal.valueOf(request.lng())
            );
            zoneId = inZone ? request.zoneId() : null;
        } else {
            ZoneDTO zone = zoneService.findZoneForPoint(
                    BigDecimal.valueOf(request.lat()),
                    BigDecimal.valueOf(request.lng())
            );
            inZone = zone != null;
            if (zone != null) {
                zoneId = zone.getId();
            }
        }

        return ResponseEntity.ok(new com.speedline.location.dto.ZoneCheckResponse(inZone, zoneId));
    }

    /**
     * Trouver la zone pour un point
     */
    @GetMapping("/find")
    public ResponseEntity<ZoneDTO> findZoneForPoint(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        log.debug("Recherche de zone pour le point: lat={}, lon={}", latitude, longitude);
        ZoneDTO zone = zoneService.findZoneForPoint(latitude, longitude);
        if (zone == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(zone);
    }

    /**
     * Obtenir les frais de livraison pour un point
     */
    @GetMapping("/delivery-fee")
    public ResponseEntity<Map<String, BigDecimal>> getDeliveryFeeForPoint(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        log.debug("Récupération des frais de livraison pour le point: lat={}, lon={}", latitude, longitude);
        BigDecimal fee = zoneService.getDeliveryFeeForPoint(latitude, longitude);
        if (fee == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("deliveryFee", fee));
    }

    /**
     * Exporter toutes les zones en GeoJSON (sans pagination)
     */
    @GetMapping(value = "/export", produces = "application/geo+json")
    public ResponseEntity<String> exportZones() {
        log.info("Export GeoJSON de toutes les zones");
        String geojson = zoneService.exportZonesAsGeoJson();
        String filename = "zones-" + LocalDate.now() + ".geojson";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/geo+json"))
                .body(geojson);
    }

    /**
     * Importer des zones depuis un GeoJSON FeatureCollection
     */
    @PostMapping(value = "/import", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Integer>> importZones(@RequestBody String geojson) {
        log.info("Import GeoJSON de zones");
        Map<String, Integer> result = zoneService.importZonesFromGeoJson(geojson);
        return ResponseEntity.ok(result);
    }

    /**
     * Synchronisation interne: met à jour les affectations d'un livreur sur les zones.
     */
    @PostMapping("/sync-courier-zones")
    public ResponseEntity<Void> syncCourierZones(
            @RequestParam Long courierId,
            @RequestParam(required = false) List<Long> zoneIds) {
        zoneService.syncCourierZones(courierId, zoneIds);
        return ResponseEntity.ok().build();
    }
}
