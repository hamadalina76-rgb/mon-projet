package com.speedline.delivery.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité TrackingPoint - Points GPS de tracking
 */
@Entity
@Table(name = "tracking_points", indexes = {
    @Index(name = "idx_tracking_delivery", columnList = "deliveryId"),
    @Index(name = "idx_tracking_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers la livraison
     */
    @Column(nullable = false)
    private Long deliveryId;

    /**
     * Latitude GPS
     */
    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    /**
     * Longitude GPS
     */
    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    /**
     * Précision de la position (en mètres)
     */
    @Column(precision = 6, scale = 2)
    private BigDecimal accuracy;

    /**
     * Altitude (en mètres)
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal altitude;

    /**
     * Vitesse (en km/h)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal speed;

    /**
     * Direction (bearing) en degrés (0-360)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal bearing;

    /**
     * Niveau de batterie du téléphone (0-100)
     */
    private Integer batteryLevel;

    /**
     * Statut de la livraison au moment de ce point
     */
    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    /**
     * Timestamp du point GPS
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Calculer la distance vers un autre point (en km)
     * Formule de Haversine simplifiée
     */
    public double distanceTo(TrackingPoint other) {
        double lat1 = Math.toRadians(this.latitude.doubleValue());
        double lon1 = Math.toRadians(this.longitude.doubleValue());
        double lat2 = Math.toRadians(other.latitude.doubleValue());
        double lon2 = Math.toRadians(other.longitude.doubleValue());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.pow(Math.sin(dLat / 2), 2) +
                   Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));

        // Rayon de la Terre en km
        double r = 6371;

        return c * r;
    }

    /**
     * Calculer le cap vers un autre point (en degrés)
     */
    public double bearingTo(TrackingPoint other) {
        double lat1 = Math.toRadians(this.latitude.doubleValue());
        double lon1 = Math.toRadians(this.longitude.doubleValue());
        double lat2 = Math.toRadians(other.latitude.doubleValue());
        double lon2 = Math.toRadians(other.longitude.doubleValue());

        double dLon = lon2 - lon1;

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }
}
