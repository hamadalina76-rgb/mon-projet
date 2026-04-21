package com.speedline.delivery.service.impl;

import com.speedline.delivery.compensation.LateDeliveryCompensationService;
import com.speedline.delivery.domain.Delivery;
import com.speedline.delivery.domain.DeliveryStatus;
import com.speedline.delivery.dto.DeliveryDTO;
import com.speedline.delivery.repository.DeliveryRepository;
import com.speedline.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implémentation du service de gestion des livraisons
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final LateDeliveryCompensationService lateDeliveryCompensationService;

    // ==================== CRÉATION ====================

    @Override
    @Transactional
    public DeliveryDTO createDelivery(Long orderId, String orderNumber, String customerName, String customerPhone,
                                      String partnerName, BigDecimal pickupLat, BigDecimal pickupLon, String pickupAddress,
                                      BigDecimal dropoffLat, BigDecimal dropoffLon, String dropoffAddress,
                                      String deliveryInstructions, BigDecimal deliveryFee) {
        // TODO: Implémenter la création d'une livraison
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== LECTURE ====================

    @Override
    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryById(Long deliveryId) {
        // TODO: Implémenter la récupération par ID
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryByOrderId(Long orderId) {
        // TODO: Implémenter la récupération par orderId
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== WORKFLOW LIVREUR ====================

    @Override
    @Transactional
    public DeliveryDTO acceptDelivery(Long deliveryId, Long courierId) {
        // TODO: Implémenter l'acceptation de livraison par le livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO declineDelivery(Long deliveryId, Long courierId, String reason) {
        // TODO: Implémenter le refus de livraison par le livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO arrivedAtPickup(Long deliveryId, Long courierId) {
        // TODO: Implémenter l'arrivée au point de pickup
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO pickupOrder(Long deliveryId, Long courierId) {
        // TODO: Implémenter la récupération de la commande
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO startDelivery(Long deliveryId, Long courierId) {
        // TODO: Implémenter le début de la livraison
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO arrivedAtDropoff(Long deliveryId, Long courierId) {
        // TODO: Implémenter l'arrivée au point de dropoff
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO completeDelivery(Long deliveryId, Long courierId, String proofImageUrl, String notes) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));

        if (courierId == null || !courierId.equals(delivery.getCourierId())) {
            throw new IllegalStateException("Le livreur ne correspond pas a la livraison");
        }

        if (delivery.getStatus() == DeliveryStatus.DELIVERED
                || delivery.getStatus() == DeliveryStatus.CANCELLED
                || delivery.getStatus() == DeliveryStatus.FAILED) {
            throw new IllegalStateException("Transition invalide vers DELIVERED depuis " + delivery.getStatus());
        }

        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setProofOfDeliveryImage(proofImageUrl);
        delivery.setDeliveryNotes(notes);
        delivery.calculateActualDuration();

        Delivery saved = deliveryRepository.save(delivery);
        lateDeliveryCompensationService.evaluateAndCompensate(saved);

        return toDTO(saved);
    }

    @Override
    @Transactional
    public DeliveryDTO cancelDelivery(Long deliveryId, String cancelledBy, String reason) {
        // TODO: Implémenter l'annulation de la livraison
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO failDelivery(Long deliveryId, Long courierId, String reason) {
        // TODO: Implémenter l'échec de livraison
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== RECHERCHE ====================

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryDTO> getCourierDeliveries(Long courierId, Pageable pageable) {
        // TODO: Implémenter la récupération des livraisons d'un livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDTO> getActiveCourierDeliveries(Long courierId) {
        // TODO: Implémenter la récupération des livraisons actives d'un livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryDTO> getDeliveriesByStatus(DeliveryStatus status, Pageable pageable) {
        // TODO: Implémenter la récupération des livraisons par statut
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDTO> getPendingDeliveries() {
        // TODO: Implémenter la récupération des livraisons en attente d'assignation
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== POURBOIRE ====================

    @Override
    @Transactional
    public DeliveryDTO addTip(Long deliveryId, BigDecimal tip) {
        // TODO: Implémenter l'ajout d'un pourboire à une livraison
        throw new UnsupportedOperationException("À implémenter");
    }

    private DeliveryDTO toDTO(Delivery delivery) {
        return DeliveryDTO.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .orderNumber(delivery.getOrderNumber())
                .courierId(delivery.getCourierId())
                .courierName(delivery.getCourierName())
                .courierPhone(delivery.getCourierPhone())
                .customerName(delivery.getCustomerName())
                .customerPhone(delivery.getCustomerPhone())
                .partnerName(delivery.getPartnerName())
                .status(delivery.getStatus())
                .pickupLatitude(delivery.getPickupLatitude())
                .pickupLongitude(delivery.getPickupLongitude())
                .pickupAddress(delivery.getPickupAddress())
                .dropoffLatitude(delivery.getDropoffLatitude())
                .dropoffLongitude(delivery.getDropoffLongitude())
                .dropoffAddress(delivery.getDropoffAddress())
                .deliveryInstructions(delivery.getDeliveryInstructions())
                .estimatedDistance(delivery.getEstimatedDistance())
                .actualDistance(delivery.getActualDistance())
                .estimatedDuration(delivery.getEstimatedDuration())
                .actualDuration(delivery.getActualDuration())
                .assignedAt(delivery.getAssignedAt())
                .acceptedAt(delivery.getAcceptedAt())
                .pickedUpAt(delivery.getPickedUpAt())
                .deliveredAt(delivery.getDeliveredAt())
                .proofOfDeliveryImage(delivery.getProofOfDeliveryImage())
                .deliveryCode(delivery.getDeliveryCode())
                .deliveryFee(delivery.getDeliveryFee())
                .tip(delivery.getTip())
                .courierEarnings(delivery.getCourierEarnings())
                .build();
    }
}
