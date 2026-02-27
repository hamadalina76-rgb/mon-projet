package com.speedline.location.controller;

import com.speedline.location.dto.GeocodeRequest;
import com.speedline.location.dto.ReverseGeocodeResponse;
import com.speedline.location.dto.SaveAddressRequest;
import com.speedline.location.service.GeolocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller pour Location
 *
 * Endpoints:
 * GET    /locations/nearby-partners   - Partenaires proches
 * POST   /locations/calculate-distance - Calculer distance
 * POST   /locations/reverse-geocode   - Coordonnées → Adresse
 * POST   /locations/geocode            - Adresse → Coordonnées
 * GET    /locations/route             - Calculer itinéraire
 */
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class LocationController implements LocationApi {

    private final GeolocationService geolocationService;

    /**
     * Géocodage inverse: convertit des coordonnées GPS en adresse lisible.
     * Utilisé par l'app Flutter après détection GPS pour afficher l'adresse.
     *
     * @param request Latitude et longitude GPS
     * @return Adresse détaillée (rue, ville, gouvernorat, pays)
     */
    @Override
    @PostMapping("/reverse-geocode")
    public ResponseEntity<ReverseGeocodeResponse> reverseGeocode(
            @Valid @RequestBody GeocodeRequest request) {
        log.info("POST /locations/reverse-geocode - lat={}, lon={}",
                request.getLatitude(), request.getLongitude());

        ReverseGeocodeResponse response = geolocationService.reverseGeocode(
                request.getLatitude(), request.getLongitude());

        return ResponseEntity.ok(response);
    }

        @Override
        @GetMapping("/nearby-partners")
        public ResponseEntity<List<Map<String, Object>>> getNearbyPartners(
                @RequestParam("lat") BigDecimal latitude,
                @RequestParam("lon") BigDecimal longitude,
                @RequestParam(value = "radius", required = false) Integer radiusMeters) {
                int radius = radiusMeters == null ? 5000 : radiusMeters;
                List<GeolocationService.NearbyPartnerDTO> partners = geolocationService.findNearbyPartners(latitude, longitude, radius);
                List<Map<String, Object>> out = partners.stream()
                                .map(p -> Map.<String, Object>of(
                                                "partnerId", p.partnerId(),
                                                "name", p.name(),
                                                "distanceKm", p.distanceKm(),
                                                "etaMinutes", p.estimatedDeliveryTime()
                                ))
                                .collect(Collectors.toList());
                return ResponseEntity.ok(out);
        }

        @Override
        @PostMapping("/calculate-distance")
        public ResponseEntity<Map<String, Object>> calculateDistance(@RequestBody Map<String, Object> payload) {
                // Expect payload to contain fromLat, fromLon, toLat, toLon
                try {
                        BigDecimal fromLat = new BigDecimal(payload.get("fromLat").toString());
                        BigDecimal fromLon = new BigDecimal(payload.get("fromLon").toString());
                        BigDecimal toLat = new BigDecimal(payload.get("toLat").toString());
                        BigDecimal toLon = new BigDecimal(payload.get("toLon").toString());

                        GeolocationService.DistanceResult res = geolocationService.calculateDistance(fromLat, fromLon, toLat, toLon);
                        Map<String, Object> out = Map.of(
                                        "distanceKm", res.distanceKm(),
                                        "durationMinutes", res.durationMinutes()
                        );
                        return ResponseEntity.ok(out);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload"));
                }
        }

        @Override
        @GetMapping("/route")
        public ResponseEntity<Map<String, Object>> getRoute(
                @RequestParam("fromLat") BigDecimal fromLat,
                @RequestParam("fromLon") BigDecimal fromLon,
                @RequestParam("toLat") BigDecimal toLat,
                @RequestParam("toLon") BigDecimal toLon) {
                GeolocationService.RouteResult r = geolocationService.calculateRoute(fromLat, fromLon, toLat, toLon, "driving");
                Map<String, Object> out = Map.of(
                                "distanceKm", r.distanceKm(),
                                "durationMinutes", r.durationMinutes(),
                                "polyline", r.encodedPolyline(),
                                "instructions", r.instructions()
                );
                return ResponseEntity.ok(out);
        }

        @Override
        @PostMapping("/customer-address")
        public ResponseEntity<Map<String, Object>> saveCustomerAddress(
                        @RequestBody SaveAddressRequest request) {
                log.info("POST /locations/customer-address - user={}, city={}",
                                request.getUserId(), request.getCity());
                GeolocationService.SavedAddressDTO saved = geolocationService.saveCustomerAddress(request);
                Map<String, Object> out = new java.util.LinkedHashMap<>();
                out.put("id", saved.id());
                out.put("formattedAddress", saved.formattedAddress());
                out.put("city", saved.city() != null ? saved.city() : "");
                out.put("latitude", saved.latitude());
                out.put("longitude", saved.longitude());
                return ResponseEntity.ok(out);
        }

    // TODO: Implémenter les autres endpoints
    // GET /locations/nearby-partners
    // POST /locations/calculate-distance
    // GET /locations/route
}
