package com.speedline.location.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.location.domain.Zone;
import com.speedline.location.dto.ZoneDTO;
import com.speedline.location.dto.ZoneGeometryDTO;
import com.speedline.location.exception.InvalidBoundaryException;
import com.speedline.location.exception.ZoneNotFoundException;
import com.speedline.location.exception.ZoneOverlapException;
import com.speedline.location.repository.ZoneRepository;
import com.speedline.location.service.ZoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des zones
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ZoneServiceImpl implements ZoneService {

    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public ZoneDTO createZone(String name, String description, String city, Zone.ZoneType type,
                             String boundaryJson, BigDecimal deliveryFee,
                             Integer minDeliveryTime, Integer maxDeliveryTime) {
        log.info("Création d'une nouvelle zone: {}", name);
        
        // Valider le polygone et convertir en GeoJSON si nécessaire
        String geoJsonBoundary = toGeoJsonPolygon(boundaryJson);
        validateBoundary(geoJsonBoundary);
        
        // Vérifier les chevauchements (ignore si des zones existantes ont un format invalide)
        try {
            checkOverlaps(geoJsonBoundary, null);
        } catch (DataAccessException e) {
            log.warn("Impossible de vérifier les chevauchements (zones existantes au format legacy?) : {}", e.getMessage());
        }
        
        // Vérifier nom unique (par ville)
        zoneRepository.findByNameAndCity(name, city).ifPresent(z -> {
            throw new InvalidBoundaryException("Une zone avec le nom '" + name + "' existe déjà dans cette ville");
        });
        
        Zone zone = Zone.builder()
                .name(name)
                .description(description)
                .city(city)
                .type(type)
                .boundaryJson(geoJsonBoundary)
                .deliveryFee(deliveryFee)
                .minDeliveryTime(minDeliveryTime)
                .maxDeliveryTime(maxDeliveryTime)
                .isActive(true)
                .build();
        
        zone = zoneRepository.save(zone);
        log.info("Zone créée avec succès: ID={}", zone.getId());
        
        return mapToDTO(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneDTO getZoneById(Long zoneId) {
        log.debug("Récupération de la zone ID: {}", zoneId);
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
        return mapToDTO(zone);
    }

    @Override
    @Transactional
    public ZoneDTO updateZone(Long zoneId, String name, String description,
                              BigDecimal deliveryFee, String boundaryJson) {
        log.info("Mise à jour de la zone ID: {}", zoneId);
        
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
        
        // Valider le polygone si fourni
        if (boundaryJson != null && !boundaryJson.isEmpty()) {
            String geoJsonBoundary = toGeoJsonPolygon(boundaryJson);
            validateBoundary(geoJsonBoundary);
            try {
                checkOverlaps(geoJsonBoundary, zoneId);
            } catch (DataAccessException e) {
                log.warn("Impossible de vérifier les chevauchements : {}", e.getMessage());
            }
            zone.setBoundaryJson(geoJsonBoundary);
        }
        
        if (name != null && !name.isEmpty()) {
            zone.setName(name);
        }
        if (description != null) {
            zone.setDescription(description);
        }
        if (deliveryFee != null) {
            zone.setDeliveryFee(deliveryFee);
        }
        
        zone = zoneRepository.save(zone);
        log.info("Zone mise à jour avec succès: ID={}", zoneId);
        
        return mapToDTO(zone);
    }

    @Override
    @Transactional
    public ZoneDTO setActiveStatus(Long zoneId, boolean isActive) {
        log.info("Changement du statut de la zone ID: {} -> {}", zoneId, isActive);
        
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
        
        zone.setIsActive(isActive);
        zone = zoneRepository.save(zone);
        
        return mapToDTO(zone);
    }

    @Override
    @Transactional
    public void deleteZone(Long zoneId) {
        log.info("Suppression de la zone ID: {}", zoneId);
        
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
        
        zoneRepository.delete(zone);
        log.info("Zone supprimée avec succès: ID={}", zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZoneDTO> getActiveZones() {
        log.debug("Récupération de toutes les zones actives");
        return zoneRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ZoneDTO> getAllZones(Pageable pageable, String search, Boolean isActive) {
        log.debug("Récupération paginée des zones, search={}, isActive={}", search, isActive);
        boolean hasSearch = search != null && !search.isBlank();
        boolean filterByActive = isActive != null;

        Page<Zone> zones;
        if (hasSearch && filterByActive) {
            zones = zoneRepository.searchByNameOrCityAndIsActive(search.trim(), isActive, pageable);
        } else if (hasSearch) {
            zones = zoneRepository.searchByNameOrCity(search.trim(), pageable);
        } else if (Boolean.TRUE.equals(isActive)) {
            zones = zoneRepository.findByIsActiveTrue(pageable);
        } else if (Boolean.FALSE.equals(isActive)) {
            zones = zoneRepository.findByIsActiveFalse(pageable);
        } else {
            zones = zoneRepository.findAll(pageable);
        }

        List<ZoneDTO> dtos = zones.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, zones.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZoneDTO> getZonesByType(Zone.ZoneType type) {
        log.debug("Récupération des zones par type: {}", type);
        return zoneRepository.findByTypeAndIsActiveTrue(type).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneDTO findZoneForPoint(BigDecimal latitude, BigDecimal longitude) {
        log.debug("Recherche de zone pour le point: lat={}, lon={}", latitude, longitude);
        return zoneRepository.findZoneForPoint(latitude, longitude)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPointInZone(Long zoneId, BigDecimal latitude, BigDecimal longitude) {
        log.debug("Vérification si le point est dans la zone ID: {}", zoneId);
        
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
        
        List<Zone> zones = zoneRepository.findZonesContainingPoint(latitude, longitude);
        return zones.stream().anyMatch(z -> z.getId().equals(zoneId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getDeliveryFeeForPoint(BigDecimal latitude, BigDecimal longitude) {
        log.debug("Récupération des frais de livraison pour le point: lat={}, lon={}", latitude, longitude);
        
        Zone zone = zoneRepository.findZoneForPoint(latitude, longitude)
                .orElse(null);
        
        return zone != null ? zone.getDeliveryFee() : null;
    }

    /**
     * Convertit [[lat,lon],...] en GeoJSON Polygon pour PostGIS.
     * Accepte aussi un GeoJSON Polygon déjà formaté.
     */
    private String toGeoJsonPolygon(String boundaryJson) {
        if (boundaryJson == null || boundaryJson.trim().isEmpty()) {
            return boundaryJson;
        }
        String trimmed = boundaryJson.trim();
        if (trimmed.startsWith("{\"type\"") && trimmed.contains("Polygon")) {
            return boundaryJson;
        }
        try {
            List<List<Double>> coords = objectMapper.readValue(boundaryJson, new TypeReference<List<List<Double>>>() {});
            if (coords == null || coords.size() < 3) {
                return boundaryJson;
            }
            List<List<Double>> ring = new ArrayList<>();
            for (List<Double> p : coords) {
                if (p != null && p.size() >= 2) {
                    ring.add(List.of(p.get(1), p.get(0)));
                }
            }
            if (ring.size() < 3) {
                return boundaryJson;
            }
            if (!ring.get(0).equals(ring.get(ring.size() - 1))) {
                ring.add(ring.get(0));
            }
            Map<String, Object> geoJson = new LinkedHashMap<>();
            geoJson.put("type", "Polygon");
            geoJson.put("coordinates", List.of(ring));
            return objectMapper.writeValueAsString(geoJson);
        } catch (Exception e) {
            log.warn("Impossible de convertir en GeoJSON, utilisation brute: {}", e.getMessage());
            return boundaryJson;
        }
    }

    /**
     * Valider le polygone de la zone (attend un GeoJSON Polygon)
     */
    private void validateBoundary(String boundaryJson) {
        if (boundaryJson == null || boundaryJson.trim().isEmpty()) {
            throw new InvalidBoundaryException("Le polygone de la zone ne peut pas être vide");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> geojson = objectMapper.readValue(boundaryJson, Map.class);
            if ("Polygon".equals(geojson.get("type"))) {
                List<?> coords = (List<?>) ((List<?>) geojson.get("coordinates")).get(0);
                if (coords == null || coords.size() < 4) {
                    throw new InvalidBoundaryException("Un polygone doit contenir au moins 3 points (ring fermé)");
                }
                for (Object o : coords) {
                    List<?> p = (List<?>) o;
                    if (p == null || p.size() < 2) {
                        throw new InvalidBoundaryException("Chaque point doit avoir [longitude, latitude]");
                    }
                    double lon = ((Number) p.get(0)).doubleValue();
                    double lat = ((Number) p.get(1)).doubleValue();
                    if (lat < -90 || lat > 90) throw new InvalidBoundaryException("Latitude invalide: " + lat);
                    if (lon < -180 || lon > 180) throw new InvalidBoundaryException("Longitude invalide: " + lon);
                }
            } else {
                List<List<Double>> coordinates = objectMapper.readValue(boundaryJson, new TypeReference<List<List<Double>>>() {});
                if (coordinates == null || coordinates.isEmpty() || coordinates.size() < 3) {
                    throw new InvalidBoundaryException("Le polygone doit contenir au moins 3 points");
                }
            }
            Boolean isValid = zoneRepository.isValidPolygon(boundaryJson);
            if (isValid == null || !isValid) {
                throw new InvalidBoundaryException("Le polygone n'est pas valide (auto-intersection possible)");
            }
        } catch (InvalidBoundaryException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la validation du polygone", e);
            throw new InvalidBoundaryException("Format JSON invalide pour le polygone: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifier les chevauchements avec d'autres zones
     */
    private void checkOverlaps(String boundaryJson, Long excludeZoneId) {
        List<Zone> overlappingZones = zoneRepository.findOverlappingZones(
                boundaryJson, 
                excludeZoneId != null ? excludeZoneId : -1L
        );
        
        if (!overlappingZones.isEmpty()) {
            Zone overlappingZone = overlappingZones.get(0);
            throw new ZoneOverlapException(overlappingZone.getId(), overlappingZone.getName());
        }
    }

    /**
     * Mapper Zone vers ZoneDTO
     */
    private ZoneDTO mapToDTO(Zone zone) {
        ZoneGeometryDTO geometry = calculateGeometry(zone.getBoundaryJson());
        
        return ZoneDTO.builder()
                .id(zone.getId())
                .name(zone.getName())
                .description(zone.getDescription())
                .city(zone.getCity())
                .type(zone.getType())
                .boundaryJson(zone.getBoundaryJson())
                .deliveryFee(zone.getDeliveryFee())
                .minDeliveryTime(zone.getMinDeliveryTime())
                .maxDeliveryTime(zone.getMaxDeliveryTime())
                .isActive(zone.getIsActive())
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .areaKm2(geometry.getAreaKm2())
                .center(geometry.getCenter())
                .partnersCount(0L) // Sera calculé via une requête séparée si nécessaire
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportZonesAsGeoJson() {
        log.info("Export de toutes les zones en GeoJSON");
        List<Zone> zones = zoneRepository.findAll();
        List<Map<String, Object>> features = new ArrayList<>();
        for (Zone z : zones) {
            if (z.getBoundaryJson() == null || z.getBoundaryJson().isBlank()) continue;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> geom = objectMapper.readValue(z.getBoundaryJson(), Map.class);
                if (!"Polygon".equals(geom.get("type"))) continue;
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("name", z.getName());
                props.put("city", z.getCity());
                props.put("type", z.getType() != null ? z.getType().name() : "DELIVERY");
                props.put("deliveryFee", z.getDeliveryFee());
                props.put("isActive", z.getIsActive() != null && z.getIsActive());
                Map<String, Object> feature = new LinkedHashMap<>();
                feature.put("type", "Feature");
                feature.put("properties", props);
                feature.put("geometry", geom);
                features.add(feature);
            } catch (Exception e) {
                log.warn("Zone ID {} ignorée pour l'export: {}", z.getId(), e.getMessage());
            }
        }
        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("type", "FeatureCollection");
        fc.put("features", features);
        try {
            return objectMapper.writeValueAsString(fc);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du GeoJSON", e);
        }
    }

    @Override
    @Transactional
    public Map<String, Integer> importZonesFromGeoJson(String geojson) {
        log.info("Import de zones depuis GeoJSON");
        int created = 0;
        int failed = 0;
        Set<String> existingNames = zoneRepository.findAll().stream()
                .map(z -> z.getName() != null ? z.getName().toLowerCase() : "")
                .filter(n -> !n.isBlank())
                .collect(Collectors.toSet());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = objectMapper.readValue(geojson, Map.class);
            List<Map<String, Object>> features = (List<Map<String, Object>>) root.get("features");
            if (features == null) {
                if ("Feature".equals(root.get("type"))) {
                    features = List.of(root);
                } else {
                    return Map.of("created", 0, "failed", 0);
                }
            }
            for (Map<String, Object> f : features) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> geom = (Map<String, Object>) f.get("geometry");
                    if (geom == null || !"Polygon".equals(geom.get("type"))) {
                        failed++;
                        continue;
                    }
                    List<?> coordsList = (List<?>) geom.get("coordinates");
                    if (coordsList == null || coordsList.isEmpty()) {
                        failed++;
                        continue;
                    }
                    List<?> ring = (List<?>) coordsList.get(0);
                    if (ring == null || ring.size() < 4) {
                        failed++;
                        continue;
                    }
                    String boundaryJson = objectMapper.writeValueAsString(geom);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> props = (Map<String, Object>) f.get("properties");
                    props = props != null ? props : new LinkedHashMap<>();
                    String baseName = props.containsKey("name") && props.get("name") != null
                            ? String.valueOf(props.get("name")).trim()
                            : "Zone importée";
                    String name = baseName;
                    int suffix = 0;
                    while (existingNames.contains(name.toLowerCase())) {
                        suffix++;
                        name = baseName + " " + suffix;
                    }
                    existingNames.add(name.toLowerCase());
                    String city = props.containsKey("city") && props.get("city") != null
                            ? String.valueOf(props.get("city")).trim()
                            : "Tunis";
                    Zone.ZoneType type = Zone.ZoneType.DELIVERY;
                    if (props.containsKey("type") && props.get("type") != null) {
                        try {
                            type = Zone.ZoneType.valueOf(String.valueOf(props.get("type")).toUpperCase());
                        } catch (Exception ignored) {}
                    }
                    BigDecimal deliveryFee = BigDecimal.ZERO;
                    if (props.containsKey("deliveryFee") && props.get("deliveryFee") != null) {
                        try {
                            deliveryFee = new BigDecimal(String.valueOf(props.get("deliveryFee")));
                        } catch (Exception ignored) {}
                    }
                    boolean isActive = props.get("isActive") == null || Boolean.TRUE.equals(props.get("isActive"));
                    createZone(name, null, city, type, boundaryJson, deliveryFee, 30, 60);
                    created++;
                } catch (Exception e) {
                    log.warn("Échec import d'une feature: {}", e.getMessage());
                    failed++;
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du parsing du GeoJSON", e);
            throw new InvalidBoundaryException("GeoJSON invalide: " + e.getMessage(), e);
        }
        return Map.of("created", created, "failed", failed);
    }

    /**
     * Calculer la géométrie de la zone
     */
    private ZoneGeometryDTO calculateGeometry(String boundaryJson) {
        try {
            BigDecimal areaKm2 = zoneRepository.calculateAreaKm2(boundaryJson);
            Double[] center = zoneRepository.calculateCenter(boundaryJson);
            BigDecimal perimeterKm = zoneRepository.calculatePerimeterKm(boundaryJson);
            
            // Compter les points (GeoJSON Polygon ou ancien format [[lat,lon],...])
            int pointCount = 0;
            String trimmed = boundaryJson != null ? boundaryJson.trim() : "";
            if (trimmed.startsWith("{\"type\"") && trimmed.contains("Polygon")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> geo = objectMapper.readValue(boundaryJson, Map.class);
                List<?> rings = (List<?>) geo.get("coordinates");
                if (rings != null && !rings.isEmpty()) {
                    List<?> ring = (List<?>) rings.get(0);
                    pointCount = ring != null ? ring.size() : 0;
                }
            } else {
                List<List<Double>> coords = objectMapper.readValue(boundaryJson, new TypeReference<List<List<Double>>>() {});
                pointCount = coords != null ? coords.size() : 0;
            }
            
            return ZoneGeometryDTO.builder()
                    .areaKm2(areaKm2 != null ? areaKm2.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                    .center(center)
                    .perimeterKm(perimeterKm != null ? perimeterKm.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                    .pointCount(pointCount)
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors du calcul de la géométrie", e);
            return ZoneGeometryDTO.builder()
                    .areaKm2(BigDecimal.ZERO)
                    .center(new Double[]{0.0, 0.0})
                    .perimeterKm(BigDecimal.ZERO)
                    .pointCount(0)
                    .build();
        }
    }
}
