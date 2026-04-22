package com.speedline.delivery.dispatch.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.config.runtime.RuntimeDispatchTuningService;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.dto.DispatchProposalView;
import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import feign.FeignException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SEMI_AUTO: holds proposed order–courier pairs in Redis until an operator approves or rejects.
 * Orders remain in the pending queue until approved (then removed and {@link DispatchAssignedEvent} is published as non-proposal).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchProposalService {

    private static final Duration PROPOSAL_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final DeliveryEventProducer deliveryEventProducer;
    private final CourierResponseTimeoutTracker responseTimeoutTracker;
    private final RuntimeDispatchTuningService runtimeDispatchTuningService;
    private final DispatchRealtimePublisher realtimePublisher;
    private final DispatchDeliveryRecordService dispatchDeliveryRecordService;
    private final OrderServiceClient orderServiceClient;

    /** Distinct from {@code CourierResponseTimeoutTracker}'s {@code dispatch:proposal:order:%d}. */
    private static String orderKey(Long orderId) {
        return "dispatch:semi-auto:proposal:order:" + orderId;
    }

    private static String zoneOrderIdsKey(Long zoneId) {
        return "dispatch:zone:" + zoneId + ":semi-auto:proposal-order-ids";
    }

    public boolean hasPendingProposal(Long orderId) {
        if (orderId == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(orderKey(orderId)));
    }

    public void enqueueProposal(Long zoneId, Assignment assignment, Map<Long, PendingOrder> orderById) {
        if (assignment == null || assignment.getOrderId() == null) {
            return;
        }
        try {
            ProposalPayload p = new ProposalPayload();
            p.setZoneId(zoneId);
            p.setOrderId(assignment.getOrderId());
            p.setCourierId(assignment.getCourierId());
            p.setBundleId(assignment.getBundleId());
            p.setCost(assignment.getCost());
            p.setEtaPickupMin(assignment.getEtaPickupMin());
            p.setEtaDeliveryMin(assignment.getEtaDeliveryMin());
            p.setCreatedAt(Instant.now());

            String json = objectMapper.writeValueAsString(p);
            String k = orderKey(assignment.getOrderId());
            redisTemplate.opsForValue().set(k, json, PROPOSAL_TTL);
            redisTemplate.opsForSet().add(zoneOrderIdsKey(zoneId), String.valueOf(assignment.getOrderId()));
            redisTemplate.expire(zoneOrderIdsKey(zoneId), PROPOSAL_TTL);

            DispatchProposalView view = toView(p);
            realtimePublisher.publishProposal(view);
        } catch (Exception ex) {
            log.error("Failed to enqueue dispatch proposal orderId={}: {}", assignment.getOrderId(), ex.getMessage(), ex);
        }
    }

    public List<DispatchProposalView> listForZone(Long zoneId) {
        if (zoneId == null) {
            return List.of();
        }
        Set<String> members = redisTemplate.opsForSet().members(zoneOrderIdsKey(zoneId));
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<DispatchProposalView> out = new ArrayList<>();
        for (String mid : members) {
            try {
                long oid = Long.parseLong(mid);
                loadPayload(oid).ifPresent(payload -> out.add(toView(payload)));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        out.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                return 0;
            }
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });
        return out;
    }

    public Map<String, Object> approve(Long orderId) {
        Optional<ProposalPayload> opt = loadPayload(orderId);
        if (opt.isEmpty()) {
            return Map.of("success", false, "error", "PROPOSAL_NOT_FOUND");
        }
        ProposalPayload p = opt.get();
        try {
            try {
                orderServiceClient.assignCourierToOrder(
                        p.getOrderId(), Map.of("courierId", p.getCourierId()));
            } catch (FeignException e) {
                String detail = e.contentUTF8() != null && !e.contentUTF8().isBlank()
                        ? e.contentUTF8()
                        : e.getMessage();
                log.warn("approve: order-service rejected orderId={} status={} {}", p.getOrderId(), e.status(), detail);
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("error", "ORDER_ASSIGN_FAILED");
                err.put("httpStatus", e.status());
                err.put("message", detail);
                return err;
            }
            PendingOrder pendingOrder = pendingOrderRedisRepository.findByZone(p.getZoneId()).stream()
                    .filter(o -> o != null && orderId.equals(o.getId()))
                    .findFirst()
                    .orElseGet(() -> {
                        PendingOrder stub = new PendingOrder();
                        stub.setId(p.getOrderId());
                        stub.setZoneId(p.getZoneId());
                        return stub;
                    });
            pendingOrderRedisRepository.remove(orderId);
            Assignment forDb = Assignment.builder()
                    .orderId(p.getOrderId())
                    .courierId(p.getCourierId())
                    .bundleId(p.getBundleId())
                    .cost(p.getCost())
                    .etaPickupMin(p.getEtaPickupMin())
                    .etaDeliveryMin(p.getEtaDeliveryMin())
                    .build();
            dispatchDeliveryRecordService.createFromAssignment(pendingOrder, forDb);
            deliveryEventProducer.publishAssigned(DispatchAssignedEvent.builder()
                    .zoneId(p.getZoneId())
                    .orderId(p.getOrderId())
                    .courierId(p.getCourierId())
                    .bundleId(p.getBundleId())
                    .cost(p.getCost())
                    .etaPickupMin(p.getEtaPickupMin())
                    .etaDeliveryMin(p.getEtaDeliveryMin())
                    .dispatchMode(DispatchMode.SEMI_AUTO)
                    .proposal(false)
                    .build());

            responseTimeoutTracker.trackProposal(
                    p.getOrderId(),
                    p.getCourierId(),
                    Duration.ofSeconds(runtimeDispatchTuningService.responseTimeoutDeadlineSeconds()),
                    pendingOrder);

            redisTemplate.opsForValue().set(
                    "courier:" + p.getCourierId() + ":lastAssignedAt",
                    Instant.now().toString(),
                    Duration.ofHours(24));

            removeFromStore(p.getZoneId(), orderId);
            realtimePublisher.publishProposalResolved(p.getZoneId(), orderId, "APPROVED");
            return Map.of("success", true, "orderId", orderId, "courierId", p.getCourierId());
        } catch (Exception ex) {
            log.error("approve proposal failed orderId={}: {}", orderId, ex.getMessage(), ex);
            return Map.of("success", false, "error", ex.getClass().getSimpleName());
        }
    }

    public Map<String, Object> reject(Long orderId) {
        Optional<ProposalPayload> opt = loadPayload(orderId);
        if (opt.isEmpty()) {
            return Map.of("success", false, "error", "PROPOSAL_NOT_FOUND");
        }
        ProposalPayload p = opt.get();
        removeFromStore(p.getZoneId(), orderId);
        realtimePublisher.publishProposalResolved(p.getZoneId(), orderId, "REJECTED");
        return Map.of("success", true, "orderId", orderId, "mode", "REJECTED");
    }

    private void removeFromStore(Long zoneId, Long orderId) {
        redisTemplate.delete(orderKey(orderId));
        if (zoneId != null) {
            redisTemplate.opsForSet().remove(zoneOrderIdsKey(zoneId), String.valueOf(orderId));
        }
    }

    private Optional<ProposalPayload> loadPayload(Long orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        String raw = redisTemplate.opsForValue().get(orderKey(orderId));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, ProposalPayload.class));
        } catch (Exception ex) {
            log.debug("parse proposal failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static DispatchProposalView toView(ProposalPayload p) {
        return DispatchProposalView.builder()
                .zoneId(p.getZoneId())
                .orderId(p.getOrderId())
                .courierId(p.getCourierId())
                .bundleId(p.getBundleId())
                .cost(p.getCost())
                .etaPickupMin(p.getEtaPickupMin())
                .etaDeliveryMin(p.getEtaDeliveryMin())
                .createdAt(p.getCreatedAt())
                .build();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProposalPayload {
        private Long zoneId;
        private Long orderId;
        private Long courierId;
        private Long bundleId;
        private Double cost;
        private Integer etaPickupMin;
        private Integer etaDeliveryMin;
        private Instant createdAt;
    }
}
