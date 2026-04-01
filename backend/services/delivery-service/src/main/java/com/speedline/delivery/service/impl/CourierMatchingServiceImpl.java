package com.speedline.delivery.service.impl;

import com.speedline.delivery.domain.Delivery;
import com.speedline.delivery.domain.DeliveryStatus;
import com.speedline.delivery.dto.DeliveryDTO;
import com.speedline.delivery.repository.DeliveryRepository;
import com.speedline.delivery.service.CourierMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implémentation du service de matching livreur-commande.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourierMatchingServiceImpl implements CourierMatchingService {

    private final DeliveryRepository deliveryRepository;
    // TODO: Injecter UserServiceClient / LocationServiceClient pour récupérer les livreurs et leurs positions

    @Override
    @Transactional(readOnly = true)
    public Long findBestCourier(Long deliveryId) {
        // TODO: Implémenter l'algorithme pour trouver le meilleur livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAvailableCouriers(BigDecimal pickupLatitude, BigDecimal pickupLongitude, double radiusKm) {
        // Fallback minimal: on se base sur l'historique des livreurs ayant deja livre.
        return deliveryRepository.findReassignmentCandidateCourierIds(-1L);
    }

    @Override
    @Transactional
    public DeliveryDTO autoAssignCourier(Long deliveryId) {
        // TODO: Implémenter l'assignation automatique d'un livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO assignCourier(Long deliveryId, Long courierId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));

        delivery.setCourierId(courierId);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());
        return toDTO(deliveryRepository.save(delivery));
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateCourierScore(Long courierId, Long deliveryId) {
        // TODO: Implémenter le calcul du score d'un livreur pour une livraison
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO reassignDelivery(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));

        Long currentCourierId = delivery.getCourierId();
        Long nextCourierId = pickNextCourier(currentCourierId);

        if (nextCourierId == null) {
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setCourierId(null);
            delivery.setCourierName(null);
            delivery.setCourierPhone(null);
            delivery.setAssignedAt(null);
            delivery.setAcceptedAt(null);
        } else {
            delivery.setCourierId(nextCourierId);
            delivery.setStatus(DeliveryStatus.ASSIGNED);
            delivery.setAssignedAt(LocalDateTime.now());
            delivery.setAcceptedAt(null);
        }

        return toDTO(deliveryRepository.save(delivery));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCourierAcceptDelivery(Long courierId) {
        return getCourierWorkload(courierId) == 0;
    }

    @Override
    @Transactional(readOnly = true)
    public int getCourierWorkload(Long courierId) {
        return deliveryRepository.findActiveDeliveriesByCourier(courierId).size();
    }

    @Override
    @Transactional
    public Map<String, Object> reassignOnCourierUnavailability(Long courierId) {
        // Ne traite que les statuts sensibles a la course simultanee (TC-US3-5).
        List<Delivery> impacted = deliveryRepository.findActiveDeliveriesByCourier(courierId)
                .stream()
                .filter(d -> d.getStatus() == DeliveryStatus.ASSIGNED || d.getStatus() == DeliveryStatus.ACCEPTED)
                .sorted(Comparator.comparing(Delivery::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<Long> reassignedDeliveryIds = new ArrayList<>();
        List<Long> pendingDeliveryIds = new ArrayList<>();

        for (Delivery delivery : impacted) {
            // Idempotence soft: si deja bougee par un autre thread, on skip.
            if (!courierId.equals(delivery.getCourierId())) {
                continue;
            }

            Long replacementCourierId = pickNextCourier(courierId);
            if (replacementCourierId == null) {
                delivery.setStatus(DeliveryStatus.PENDING);
                delivery.setCourierId(null);
                delivery.setCourierName(null);
                delivery.setCourierPhone(null);
                delivery.setAssignedAt(null);
                delivery.setAcceptedAt(null);
                pendingDeliveryIds.add(delivery.getId());
                log.warn("No replacement courier found for delivery {} after courier {} became unavailable", delivery.getId(), courierId);
            } else {
                delivery.setCourierId(replacementCourierId);
                delivery.setStatus(DeliveryStatus.ASSIGNED);
                delivery.setAssignedAt(LocalDateTime.now());
                delivery.setAcceptedAt(null);
                reassignedDeliveryIds.add(delivery.getId());
                log.info("Delivery {} reassigned from courier {} to {}", delivery.getId(), courierId, replacementCourierId);
            }
            deliveryRepository.save(delivery);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sourceCourierId", courierId);
        result.put("processedCount", impacted.size());
        result.put("reassignedDeliveryIds", reassignedDeliveryIds);
        result.put("pendingDeliveryIds", pendingDeliveryIds);
        return result;
    }

    private Long pickNextCourier(Long excludedCourierId) {
        List<Long> candidates = deliveryRepository.findReassignmentCandidateCourierIds(excludedCourierId);
        for (Long candidateId : candidates) {
            if (candidateId == null) {
                continue;
            }
            if (canCourierAcceptDelivery(candidateId)) {
                return candidateId;
            }
        }
        return null;
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

