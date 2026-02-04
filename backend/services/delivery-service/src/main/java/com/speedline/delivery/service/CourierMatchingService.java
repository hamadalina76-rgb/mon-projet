package com.speedline.delivery.service;

import com.speedline.delivery.dto.DeliveryDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour le matching livreur-commande
 * Algorithme d'assignation intelligente des livraisons
 */
public interface CourierMatchingService {

    /**
     * Trouver le meilleur livreur pour une livraison
     * Critères: distance, disponibilité, charge de travail, note, type de véhicule
     * 
     * @param deliveryId ID de la livraison
     * @return ID du livreur optimal, ou null si aucun disponible
     */
    Long findBestCourier(Long deliveryId);

    /**
     * Trouver les livreurs disponibles pour une livraison
     * 
     * @param pickupLatitude Latitude du point de pickup
     * @param pickupLongitude Longitude du point de pickup
     * @param radiusKm Rayon de recherche en km
     * @return Liste des IDs de livreurs disponibles, triés par pertinence
     */
    List<Long> findAvailableCouriers(BigDecimal pickupLatitude, BigDecimal pickupLongitude, double radiusKm);

    /**
     * Assigner automatiquement un livreur à une livraison
     * 
     * @param deliveryId ID de la livraison
     * @return DeliveryDTO avec livreur assigné, ou exception si aucun disponible
     * @throws NoCourierAvailableException si aucun livreur n'est disponible
     */
    DeliveryDTO autoAssignCourier(Long deliveryId);

    /**
     * Assigner manuellement un livreur à une livraison
     * 
     * @param deliveryId ID de la livraison
     * @param courierId ID du livreur
     * @return DeliveryDTO avec livreur assigné
     * @throws CourierNotFoundException si le livreur n'existe pas
     * @throws CourierNotAvailableException si le livreur n'est pas disponible
     */
    DeliveryDTO assignCourier(Long deliveryId, Long courierId);

    /**
     * Calculer le score d'un livreur pour une livraison
     * Score basé sur: distance, note, taux de succès, charge actuelle
     * 
     * @param courierId ID du livreur
     * @param deliveryId ID de la livraison
     * @return Score de 0 à 100
     */
    int calculateCourierScore(Long courierId, Long deliveryId);

    /**
     * Réassigner une livraison à un autre livreur
     * Utilisé en cas de refus ou d'annulation
     * 
     * @param deliveryId ID de la livraison
     * @return DeliveryDTO avec nouveau livreur
     */
    DeliveryDTO reassignDelivery(Long deliveryId);

    /**
     * Vérifier si un livreur peut accepter une nouvelle livraison
     * 
     * @param courierId ID du livreur
     * @return boolean true si le livreur peut accepter
     */
    boolean canCourierAcceptDelivery(Long courierId);

    /**
     * Obtenir la charge de travail actuelle d'un livreur
     * 
     * @param courierId ID du livreur
     * @return Nombre de livraisons actives
     */
    int getCourierWorkload(Long courierId);
}
