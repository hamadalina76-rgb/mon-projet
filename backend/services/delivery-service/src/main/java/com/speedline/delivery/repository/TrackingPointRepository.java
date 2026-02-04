package com.speedline.delivery.repository;

import com.speedline.delivery.domain.TrackingPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour TrackingPoint
 */
@Repository
public interface TrackingPointRepository extends JpaRepository<TrackingPoint, Long> {

    /**
     * Trouver tous les points de tracking d'une livraison
     */
    List<TrackingPoint> findByDeliveryIdOrderByTimestampAsc(Long deliveryId);

    /**
     * Trouver le dernier point de tracking d'une livraison
     */
    Optional<TrackingPoint> findFirstByDeliveryIdOrderByTimestampDesc(Long deliveryId);

    /**
     * Supprimer les points de tracking d'une livraison
     */
    void deleteByDeliveryId(Long deliveryId);

    /**
     * Compter les points de tracking d'une livraison
     */
    long countByDeliveryId(Long deliveryId);

    /**
     * Trouver les points de tracking dans une période
     */
    List<TrackingPoint> findByDeliveryIdAndTimestampBetween(Long deliveryId, 
                                                            LocalDateTime start, 
                                                            LocalDateTime end);

    /**
     * Calculer la distance totale parcourue (approximation)
     */
    @Query("SELECT COUNT(t) FROM TrackingPoint t WHERE t.deliveryId = :deliveryId")
    long countPointsByDelivery(@Param("deliveryId") Long deliveryId);
}
