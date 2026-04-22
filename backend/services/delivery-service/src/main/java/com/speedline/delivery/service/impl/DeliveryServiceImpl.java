package com.speedline.delivery.service.impl;

import com.speedline.delivery.client.UserServiceClient;
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
import java.util.Map;

/**
 * Implémentation du service de gestion des livraisons
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private static final List<DeliveryStatus> ACTIVE_STATUSES = List.of(
            DeliveryStatus.ASSIGNED,
            DeliveryStatus.ACCEPTED,
            DeliveryStatus.ARRIVED_AT_PICKUP,
            DeliveryStatus.PICKED_UP,
            DeliveryStatus.IN_TRANSIT,
            DeliveryStatus.ARRIVED_AT_DROPOFF
    );

    private final DeliveryRepository deliveryRepository;
    private final LateDeliveryCompensationService lateDeliveryCompensationService;
    private final UserServiceClient userServiceClient;

    // ==================== CRÉATION ====================

    @Override
    @Transactional
    public DeliveryDTO createDelivery(Long orderId, String orderNumber, String customerName, String customerPhone,
                                      String partnerName, BigDecimal pickupLat, BigDecimal pickupLon, String pickupAddress,
                                      BigDecimal dropoffLat, BigDecimal dropoffLon, String dropoffAddress,
                                      String deliveryInstructions, BigDecimal deliveryFee,
                                      Long assignedCourierId, Long bundleId,
                                      Integer etaPickupMin, Integer etaDeliveryMin) {
        if (deliveryRepository.existsByOrderId(orderId)) {
            log.debug("Delivery already exists for orderId={}, returning existing", orderId);
            return getDeliveryByOrderId(orderId);
        }

        Delivery.DeliveryBuilder b = Delivery.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .partnerName(partnerName)
                .pickupLatitude(pickupLat)
                .pickupLongitude(pickupLon)
                .pickupAddress(pickupAddress)
                .dropoffLatitude(dropoffLat)
                .dropoffLongitude(dropoffLon)
                .dropoffAddress(dropoffAddress)
                .deliveryInstructions(deliveryInstructions)
                .deliveryFee(deliveryFee != null ? deliveryFee : BigDecimal.ZERO)
                .bundleId(bundleId);

        if (assignedCourierId != null) {
            b.courierId(assignedCourierId)
                    .status(DeliveryStatus.ASSIGNED)
                    .assignedAt(LocalDateTime.now());
            fillCourierProfile(b, assignedCourierId);
            int est = etaDeliveryMin != null ? etaDeliveryMin : 0;
            if (etaPickupMin != null && etaDeliveryMin != null) {
                est = Math.max(etaDeliveryMin, etaPickupMin);
            } else if (etaDeliveryMin != null) {
                est = etaDeliveryMin;
            }
            if (est > 0) {
                b.estimatedDuration(est);
            }
        } else {
            b.status(DeliveryStatus.PENDING);
        }

        Delivery saved = deliveryRepository.save(b.build());
        return toDTO(saved);
    }

    private void fillCourierProfile(Delivery.DeliveryBuilder b, Long courierId) {
        try {
            Map<String, Object> c = userServiceClient.getCourierById(courierId);
            if (c == null) {
                return;
            }
            String first = stringVal(c.get("firstName"));
            String last = stringVal(c.get("lastName"));
            String name = null;
            if (first != null || last != null) {
                name = (String.valueOf(first == null ? "" : first) + " " + String.valueOf(last == null ? "" : last)).trim();
            }
            if (name == null || name.isBlank()) {
                name = stringVal(c.get("displayName"));
            }
            if (name != null) {
                b.courierName(name.trim());
            }
            String phone = firstString(c, "phone", "mobile", "phoneNumber");
            if (phone != null) {
                b.courierPhone(phone);
            }
        } catch (Exception ex) {
            log.debug("Could not load courier profile courierId={}: {}", courierId, ex.getMessage());
        }
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static String firstString(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    // ==================== LECTURE ====================

    @Override
    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryById(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));
        return toDTO(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryByOrderId(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable pour la commande: " + orderId));
        return toDTO(delivery);
    }

    // ==================== WORKFLOW LIVREUR ====================

    @Override
    @Transactional
    public DeliveryDTO acceptDelivery(Long deliveryId, Long courierId) {
        Delivery d = requireDelivery(deliveryId);
        if (d.getCourierId() == null || !d.getCourierId().equals(courierId)) {
            throw new IllegalStateException("Le livreur ne correspond pas a la livraison");
        }
        if (d.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException("Statut incorrect pour accepter: " + d.getStatus());
        }
        d.setStatus(DeliveryStatus.ACCEPTED);
        d.setAcceptedAt(LocalDateTime.now());
        return toDTO(deliveryRepository.save(d));
    }

    @Override
    @Transactional
    public DeliveryDTO declineDelivery(Long deliveryId, Long courierId, String reason) {
        Delivery d = requireDelivery(deliveryId);
        if (d.getCourierId() == null || !d.getCourierId().equals(courierId)) {
            throw new IllegalStateException("Le livreur ne correspond pas a la livraison");
        }
        if (d.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException("Statut incorrect pour refuser: " + d.getStatus());
        }
        d.setStatus(DeliveryStatus.PENDING);
        d.setCourierId(null);
        d.setBundleId(null);
        d.setAssignedAt(null);
        d.setCourierName(null);
        d.setCourierPhone(null);
        d.setDeliveryNotes(safeNote(d.getDeliveryNotes(), "Declined: " + reason));
        return toDTO(deliveryRepository.save(d));
    }

    @Override
    @Transactional
    public DeliveryDTO arrivedAtPickup(Long deliveryId, Long courierId) {
        Delivery d = requireForCourier(deliveryId, courierId);
        if (d.getStatus() != DeliveryStatus.ACCEPTED) {
            throw new IllegalStateException("Statut incorrect pour arrivée pickup: " + d.getStatus());
        }
        d.setStatus(DeliveryStatus.ARRIVED_AT_PICKUP);
        d.setArrivedAtPickupAt(LocalDateTime.now());
        return toDTO(deliveryRepository.save(d));
    }

    @Override
    @Transactional
    public DeliveryDTO pickupOrder(Long deliveryId, Long courierId) {
        Delivery d = requireForCourier(deliveryId, courierId);
        if (d.getStatus() != DeliveryStatus.ARRIVED_AT_PICKUP) {
            throw new IllegalStateException("Statut incorrect pour ramassage: " + d.getStatus());
        }
        d.setStatus(DeliveryStatus.PICKED_UP);
        d.setPickedUpAt(LocalDateTime.now());
        return toDTO(deliveryRepository.save(d));
    }

    @Override
    @Transactional
    public DeliveryDTO startDelivery(Long deliveryId, Long courierId) {
        Delivery d = requireForCourier(deliveryId, courierId);
        if (d.getStatus() != DeliveryStatus.PICKED_UP) {
            throw new IllegalStateException("Statut incorrect pour demarrer livraison: " + d.getStatus());
        }
        d.setStatus(DeliveryStatus.IN_TRANSIT);
        d.setInTransitAt(LocalDateTime.now());
        return toDTO(deliveryRepository.save(d));
    }

    @Override
    @Transactional
    public DeliveryDTO arrivedAtDropoff(Long deliveryId, Long courierId) {
        Delivery d = requireForCourier(deliveryId, courierId);
        if (d.getStatus() != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("Statut incorrect pour arrivée client: " + d.getStatus());
        }
        d.setStatus(DeliveryStatus.ARRIVED_AT_DROPOFF);
        d.setArrivedAtDropoffAt(LocalDateTime.now());
        return toDTO(deliveryRepository.save(d));
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
        Delivery d = requireDelivery(deliveryId);
        if (!d.isCancellable()) {
            throw new IllegalStateException("Cette livraison ne peut plus etre annulee: " + d.getStatus());
        }
        d.setStatus(DeliveryStatus.CANCELLED);
        d.setCancelledAt(LocalDateTime.now());
        d.setCancelledBy(cancelledBy);
        d.setCancellationReason(reason);
        return toDTO(deliveryRepository.save(d));
    }

    @Override
    @Transactional
    public DeliveryDTO failDelivery(Long deliveryId, Long courierId, String reason) {
        Delivery d = requireForCourier(deliveryId, courierId);
        if (d.isCompleted()) {
            throw new IllegalStateException("Livraison deja terminee: " + d.getStatus());
        }
        d.setStatus(DeliveryStatus.FAILED);
        d.setDeliveryNotes(safeNote(d.getDeliveryNotes(), "Failed: " + reason));
        return toDTO(deliveryRepository.save(d));
    }

    // ==================== RECHERCHE ====================

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryDTO> getCourierDeliveries(Long courierId, Pageable pageable) {
        return deliveryRepository.findByCourierId(courierId, pageable).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDTO> getActiveCourierDeliveries(Long courierId) {
        return deliveryRepository.findByCourierIdAndStatusIn(courierId, ACTIVE_STATUSES)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryDTO> getDeliveriesByStatus(DeliveryStatus status, Pageable pageable) {
        return deliveryRepository.findByStatus(status, pageable).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDTO> getPendingDeliveries() {
        return deliveryRepository.findPendingDeliveries().stream().map(this::toDTO).toList();
    }

    // ==================== POURBOIRE ====================

    @Override
    @Transactional
    public DeliveryDTO addTip(Long deliveryId, BigDecimal tip) {
        if (tip == null || tip.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Montant de pourboire invalide");
        }
        Delivery d = requireDelivery(deliveryId);
        BigDecimal current = d.getTip() != null ? d.getTip() : BigDecimal.ZERO;
        d.setTip(current.add(tip));
        return toDTO(deliveryRepository.save(d));
    }

    private Delivery requireDelivery(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));
    }

    private Delivery requireForCourier(Long deliveryId, Long courierId) {
        Delivery d = requireDelivery(deliveryId);
        if (courierId == null || d.getCourierId() == null || !d.getCourierId().equals(courierId)) {
            throw new IllegalStateException("Le livreur ne correspond pas a la livraison");
        }
        return d;
    }

    private static String safeNote(String existing, String append) {
        if (append == null) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return append;
        }
        return existing + " | " + append;
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
