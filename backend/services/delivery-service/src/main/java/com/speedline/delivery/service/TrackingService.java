package com.speedline.delivery.service;

import com.speedline.delivery.dto.DeliveryDTO;
import com.speedline.delivery.dto.TrackingPointDTO;
import com.speedline.delivery.dto.TrackingUpdateDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour le tracking GPS en temps réel
 */
public interface TrackingService {

    /**
     * Enregistrer une mise à jour de position GPS
     * Appelé régulièrement par l'app mobile du livreur
     * 
     * @param update TrackingUpdateDTO contenant:
     *               - deliveryId: ID de la livraison
     *               - latitude, longitude: Position GPS
     *               - accuracy: Précision en mètres
     *               - speed: Vitesse en km/h
     *               - bearing: Direction en degrés
     *               - batteryLevel: Niveau de batterie
     * @throws DeliveryNotFoundException si la livraison n'existe pas
     * @throws DeliveryNotActiveException si la livraison n'est pas en cours
     */
    void recordLocation(TrackingUpdateDTO update);

    /**
     * Obtenir la dernière position connue d'une livraison
     * 
     * @param deliveryId ID de la livraison
     * @return TrackingPointDTO dernière position
     * @throws DeliveryNotFoundException si la livraison n'existe pas
     */
    TrackingPointDTO getLastLocation(Long deliveryId);

    /**
     * Obtenir l'historique de tracking d'une livraison
     * 
     * @param deliveryId ID de la livraison
     * @return List<TrackingPointDTO> tous les points de tracking
     * @throws DeliveryNotFoundException si la livraison n'existe pas
     */
    List<TrackingPointDTO> getTrackingHistory(Long deliveryId);

    /**
     * Obtenir les informations de tracking en temps réel
     * Utilisé par le client pour suivre sa commande
     * 
     * @param deliveryId ID de la livraison
     * @return DeliveryDTO avec position actuelle et ETA
     * @throws DeliveryNotFoundException si la livraison n'existe pas
     */
    DeliveryDTO getRealtimeTracking(Long deliveryId);

    /**
     * Estimer l'heure d'arrivée (ETA)
     * Basé sur la position actuelle et le trafic
     * 
     * @param deliveryId ID de la livraison
     * @return Integer ETA en minutes
     * @throws DeliveryNotFoundException si la livraison n'existe pas
     */
    Integer estimateArrivalTime(Long deliveryId);

    /**
     * Calculer la distance restante
     * 
     * @param deliveryId ID de la livraison
     * @return BigDecimal distance en km
     * @throws DeliveryNotFoundException si la livraison n'existe pas
     */
    BigDecimal calculateRemainingDistance(Long deliveryId);

    /**
     * Calculer la distance totale parcourue
     * 
     * @param deliveryId ID de la livraison
     * @return BigDecimal distance en km
     * @throws DeliveryNotFoundException si la livraison n'existe pas
     */
    BigDecimal calculateTotalDistance(Long deliveryId);

    /**
     * Vérifier si le livreur est proche du point de pickup
     * 
     * @param deliveryId ID de la livraison
     * @param thresholdMeters Seuil de proximité en mètres
     * @return boolean true si proche
     */
    boolean isCourierNearPickup(Long deliveryId, int thresholdMeters);

    /**
     * Vérifier si le livreur est proche du point de dropoff
     * 
     * @param deliveryId ID de la livraison
     * @param thresholdMeters Seuil de proximité en mètres
     * @return boolean true si proche
     */
    boolean isCourierNearDropoff(Long deliveryId, int thresholdMeters);

    /**
     * Obtenir l'itinéraire optimisé
     * 
     * @param deliveryId ID de la livraison
     * @return Route encodée (polyline)
     * @throws DeliveryNotFoundException si la livraison n'existe pas
     */
    String getOptimizedRoute(Long deliveryId);

    /**
     * Notifier le client de l'approche du livreur
     * Appelé automatiquement quand le livreur est à X minutes
     * 
     * @param deliveryId ID de la livraison
     * @param minutesAway Minutes restantes avant arrivée
     */
    void notifyCustomerOfApproach(Long deliveryId, int minutesAway);

    /**
     * Nettoyer les anciens points de tracking
     * Appelé par un scheduler pour libérer de l'espace
     * 
     * @param daysToKeep Nombre de jours à conserver
     */
    void cleanupOldTrackingPoints(int daysToKeep);
}
