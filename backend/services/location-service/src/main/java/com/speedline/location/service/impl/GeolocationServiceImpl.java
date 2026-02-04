package com.speedline.location.service.impl;

import com.speedline.location.repository.ZoneRepository;
import com.speedline.location.service.GeolocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    // TODO: Injecter PartnerRepository, MapboxService, DistanceCalculationService

    @Override
    @Transactional(readOnly = true)
    public List<NearbyPartnerDTO> findNearbyPartners(BigDecimal latitude, BigDecimal longitude, int radiusMeters) {
        // TODO: Implémenter la recherche de partenaires proches
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public DistanceResult calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        // TODO: Implémenter le calcul de distance
        throw new UnsupportedOperationException("À implémenter");
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
    public String reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        // TODO: Implémenter le géocodage inverse
        throw new UnsupportedOperationException("À implémenter");
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
