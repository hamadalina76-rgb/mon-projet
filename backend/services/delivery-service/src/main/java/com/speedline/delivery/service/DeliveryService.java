package com.speedline.delivery.service;

import com.speedline.delivery.domain.DeliveryStatus;
import com.speedline.delivery.dto.DeliveryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour la gestion des livraisons
 */
public interface DeliveryService {

    // ==================== CRÉATION ====================

    /**
     * Créer une livraison pour une commande
     * Appelé automatiquement quand une commande est confirmée
     *
     * @param assignedCourierId si non null : statut {@code ASSIGNED} + assignation immédiate (dispatch)
     * @param bundleId identifiant de bundle (optionnel)
     * @param etaPickupMin / etaDeliveryMin estimations (optionnel)
     * @return DeliveryDTO avec id, status=PENDING ou ASSIGNED, deliveryCode
     */
    DeliveryDTO createDelivery(Long orderId, String orderNumber, String customerName, String customerPhone,
                                String partnerName, BigDecimal pickupLat, BigDecimal pickupLon, String pickupAddress,
                                BigDecimal dropoffLat, BigDecimal dropoffLon, String dropoffAddress,
                                String deliveryInstructions, BigDecimal deliveryFee,
                                Long assignedCourierId, Long bundleId,
                                Integer etaPickupMin, Integer etaDeliveryMin);

    // ==================== LECTURE ====================

    /**
     * Récupérer une livraison par son ID
     */
    DeliveryDTO getDeliveryById(Long deliveryId);

    /**
     * Récupérer une livraison par l'ID de la commande
     */
    DeliveryDTO getDeliveryByOrderId(Long orderId);

    // ==================== WORKFLOW LIVREUR ====================

    /**
     * Accepter une livraison (livreur)
     * ASSIGNED -> ACCEPTED
     */
    DeliveryDTO acceptDelivery(Long deliveryId, Long courierId);

    /** Accepter par orderId — utile depuis l'app courier qui ne connaît que l'orderId */
    DeliveryDTO acceptDeliveryByOrderId(Long orderId, Long courierId);

    /**
     * Refuser une livraison (livreur)
     * Retourne la livraison en PENDING pour réassignation
     */
    DeliveryDTO declineDelivery(Long deliveryId, Long courierId, String reason);

    /** Refuser par orderId */
    DeliveryDTO declineDeliveryByOrderId(Long orderId, Long courierId, String reason);

    /**
     * Signaler l'arrivée au point de pickup
     * ACCEPTED -> ARRIVED_AT_PICKUP
     */
    DeliveryDTO arrivedAtPickup(Long deliveryId, Long courierId);

    /**
     * Signaler la récupération de la commande
     * ARRIVED_AT_PICKUP -> PICKED_UP
     */
    DeliveryDTO pickupOrder(Long deliveryId, Long courierId);

    /**
     * Commencer la livraison
     * PICKED_UP -> IN_TRANSIT
     */
    DeliveryDTO startDelivery(Long deliveryId, Long courierId);

    /**
     * Signaler l'arrivée au point de dropoff
     * IN_TRANSIT -> ARRIVED_AT_DROPOFF
     */
    DeliveryDTO arrivedAtDropoff(Long deliveryId, Long courierId);

    /**
     * Compléter la livraison
     * ARRIVED_AT_DROPOFF -> DELIVERED
     */
    DeliveryDTO completeDelivery(Long deliveryId, Long courierId, String proofImageUrl, String notes);

    /**
     * Annuler une livraison
     */
    DeliveryDTO cancelDelivery(Long deliveryId, String cancelledBy, String reason);

    /**
     * Signaler un échec de livraison
     */
    DeliveryDTO failDelivery(Long deliveryId, Long courierId, String reason);

    // ==================== RECHERCHE ====================

    /**
     * Obtenir les livraisons d'un livreur
     */
    Page<DeliveryDTO> getCourierDeliveries(Long courierId, Pageable pageable);

    /**
     * Obtenir les livraisons actives d'un livreur
     */
    List<DeliveryDTO> getActiveCourierDeliveries(Long courierId);

    /**
     * Obtenir les livraisons par statut
     */
    Page<DeliveryDTO> getDeliveriesByStatus(DeliveryStatus status, Pageable pageable);

    /**
     * Obtenir les livraisons en attente d'assignation
     */
    List<DeliveryDTO> getPendingDeliveries();

    // ==================== POURBOIRE ====================

    /**
     * Ajouter un pourboire à une livraison
     */
    DeliveryDTO addTip(Long deliveryId, BigDecimal tip);
}
