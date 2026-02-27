package com.speedline.location.service;

import com.speedline.location.dto.ReverseGeocodeResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour les opérations géospatiales
 */
public interface GeolocationService {

    /**
     * Trouver les partenaires proches d'une position
     */
    List<NearbyPartnerDTO> findNearbyPartners(BigDecimal latitude, BigDecimal longitude, int radiusMeters);

    /**
     * Calculer la distance entre deux points
     */
    DistanceResult calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2);

    /**
     * Vérifier si un point est dans une zone de livraison
     */
    boolean isInDeliveryZone(BigDecimal latitude, BigDecimal longitude);

    /**
     * Géocoder une adresse (texte -> coordonnées)
     */
    GeocodingResult geocodeAddress(String address);

    /**
     * Géocodage inverse (coordonnées -> adresse)
     */
    ReverseGeocodeResponse reverseGeocode(BigDecimal latitude, BigDecimal longitude);

    /**
     * Calculer l'itinéraire entre deux points
     */
    RouteResult calculateRoute(BigDecimal startLat, BigDecimal startLon, 
                                BigDecimal endLat, BigDecimal endLon, String mode);

    /**
     * Obtenir la zone pour une position
     */
    ZoneDTO getZoneForLocation(BigDecimal latitude, BigDecimal longitude);

    /**
     * DTO pour les partenaires proches
     */
    record NearbyPartnerDTO(Long partnerId, String name, double distanceKm, int estimatedDeliveryTime) {}

    /**
     * DTO pour le résultat de distance
     */
    record DistanceResult(double distanceKm, int durationMinutes) {}

    /**
     * DTO pour le résultat de géocodage
     */
    record GeocodingResult(BigDecimal latitude, BigDecimal longitude, String formattedAddress, String placeId) {}

    /**
     * DTO pour le résultat d'itinéraire
     */
    record RouteResult(double distanceKm, int durationMinutes, String encodedPolyline, List<String> instructions) {}

    /**
     * DTO pour les zones
     */
    record ZoneDTO(Long id, String name, String type, BigDecimal deliveryFee, Integer minDeliveryTime, Integer maxDeliveryTime) {}
}
