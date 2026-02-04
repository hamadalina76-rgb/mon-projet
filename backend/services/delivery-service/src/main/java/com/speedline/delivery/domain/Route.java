package com.speedline.delivery.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Classe Route - Informations sur l'itinéraire
 * Embeddable pour être utilisée dans Delivery
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Route {

    /**
     * Polyline encodée de l'itinéraire (format Google/Mapbox)
     */
    private String encodedPolyline;

    /**
     * Distance totale en km
     */
    private BigDecimal distanceKm;

    /**
     * Durée estimée en minutes
     */
    private Integer durationMinutes;

    /**
     * Durée avec trafic en minutes
     */
    private Integer durationInTrafficMinutes;

    /**
     * Points de passage (waypoints) en JSON
     * Format: [{"lat":36.8,"lon":10.1,"name":"Restaurant"},{"lat":36.9,"lon":10.2,"name":"Client"}]
     */
    private String waypointsJson;

    /**
     * Instructions de navigation en JSON
     * Format: [{"instruction":"Tournez à droite","distance":100,"duration":30},...]
     */
    private String instructionsJson;

    /**
     * Résumé de l'itinéraire (ex: "Via Avenue Habib Bourguiba")
     */
    private String summary;

    /**
     * Mode de transport (DRIVING, BICYCLING, WALKING)
     */
    private String travelMode;

    /**
     * Heure de dernière mise à jour de l'itinéraire
     */
    private java.time.LocalDateTime updatedAt;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Estimer l'heure d'arrivée
     */
    public java.time.LocalDateTime estimateArrivalTime() {
        Integer duration = (durationInTrafficMinutes != null) ? durationInTrafficMinutes : durationMinutes;
        if (duration == null) {
            return null;
        }
        return java.time.LocalDateTime.now().plusMinutes(duration);
    }

    /**
     * Vérifier si l'itinéraire est encore valide (moins de 5 minutes)
     */
    public boolean isStale() {
        if (updatedAt == null) return true;
        return java.time.Duration.between(updatedAt, java.time.LocalDateTime.now()).toMinutes() > 5;
    }
}
