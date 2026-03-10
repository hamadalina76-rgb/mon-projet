package com.speedline.location.service.impl;

import com.speedline.location.dto.ReverseGeocodeResponse;
import com.speedline.location.integration.MapboxClient;
import com.speedline.location.repository.ZoneRepository;
import com.speedline.location.service.GeolocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implémentation du service géospatial
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GeolocationServiceImpl implements GeolocationService {

    private final ZoneRepository zoneRepository;
    private final MapboxClient mapboxClient;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public GeolocationService.DistanceResult calculateDistance(BigDecimal lat1, BigDecimal lon1,
                                                               BigDecimal lat2, BigDecimal lon2) {
        // Use PostGIS ST_Distance (GEOGRAPHY type gives meters automatically)
        final String sql =
            "SELECT " +
            "  ST_Distance(" +
            "    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, " +
            "    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography" +
            "  ) AS dist_m";

        double distMeters = jdbcTemplate.queryForObject(
                sql,
                Double.class,
                lon1, lat1, lon2, lat2);

        double distKm  = distMeters / 1000.0;
        // ~30 km/h average urban speed → minutes
        int    durationMin = (int) Math.ceil((distKm / 30.0) * 60);

        log.info("calculateDistance ({},{})→({},{}) = {} km ≈ {} min",
                lat1, lon1, lat2, lon2, String.format("%.3f", distKm), durationMin);

        return new GeolocationService.DistanceResult(distKm, durationMin);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInDeliveryZone(BigDecimal latitude, BigDecimal longitude) {
        // TODO: Implémenter la vérification de zone
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public GeocodingResult geocodeAddress(String address) {
        // TODO: Implémenter le géocodage
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public ReverseGeocodeResponse reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        log.info("Géocodage inverse: lat={}, lon={}", latitude, longitude);
        return mapboxClient.reverseGeocode(latitude, longitude);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResult calculateRoute(BigDecimal startLat, BigDecimal startLon,
                                     BigDecimal endLat, BigDecimal endLon, String mode) {
        // TODO: Implémenter le calcul d'itinéraire
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneDTO getZoneForLocation(BigDecimal latitude, BigDecimal longitude) {
        // TODO: Implémenter la récupération de zone
        throw new UnsupportedOperationException("À implémenter");
    }

}
