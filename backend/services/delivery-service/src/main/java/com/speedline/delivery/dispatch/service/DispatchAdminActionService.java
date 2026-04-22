package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.client.LocationServiceClient;
import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.config.DispatchZoneModeStore;
import com.speedline.delivery.dispatch.config.runtime.RuntimeDispatchTuningService;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.dto.ManualAssignRequest;
import com.speedline.delivery.dispatch.dto.ManualBundleRequest;
import com.speedline.delivery.dispatch.dto.ZoneModeResponse;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import com.speedline.delivery.dispatch.event.PendingOrderEnricher;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchAdminActionService {

    private static final String MANUAL_BUNDLE_KEY = "dispatch:manual:bundle:%s";

    private final CourierResponseTimeoutTracker timeoutTracker;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final PendingOrderEnricher pendingOrderEnricher;
    private final OrderServiceClient orderServiceClient;
    private final LocationServiceClient locationServiceClient;
    private final StringRedisTemplate redisTemplate;
    private final DispatchZoneConfig zoneConfig;
    private final DispatchZoneModeStore zoneModeStore;
    private final DispatchDeliveryRecordService dispatchDeliveryRecordService;
    private final DeliveryEventProducer deliveryEventProducer;
    private final RuntimeDispatchTuningService runtimeDispatchTuningService;

    public ZoneModeResponse getZoneMode(Long zoneId) {
        return ZoneModeResponse.builder()
                .zoneId(zoneId)
                .mode(zoneConfig.getMode(zoneId))
                .runtimeOverride(zoneModeStore.getOverride(zoneId).isPresent())
                .build();
    }

    public ZoneModeResponse setZoneMode(Long zoneId, DispatchMode mode) {
        zoneModeStore.setOverride(zoneId, mode);
        return getZoneMode(zoneId);
    }

    public Map<String, Object> manualAssign(ManualAssignRequest request) {
        PendingOrder order = timeoutTracker.getTrackedOrder(request.getOrderId())
                .orElseGet(() -> resolvePendingOrderFromOrderService(request.getOrderId()));

        try {
            orderServiceClient.assignCourierToOrder(
                    request.getOrderId(), Map.of("courierId", request.getCourierId()));
        } catch (FeignException e) {
            String detail = e.contentUTF8() != null && !e.contentUTF8().isBlank()
                    ? e.contentUTF8()
                    : e.getMessage();
            log.warn("manual assign: order-service rejected orderId={} status={} body={}",
                    request.getOrderId(), e.status(), detail);
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("orderId", request.getOrderId());
            err.put("courierId", request.getCourierId());
            err.put("error", "ORDER_ASSIGN_FAILED");
            err.put("httpStatus", e.status());
            err.put("message", detail);
            return err;
        } catch (Exception ex) {
            log.error("manual assign: order-service call failed orderId={}", request.getOrderId(), ex);
            return Map.of(
                    "success", false,
                    "orderId", request.getOrderId(),
                    "courierId", request.getCourierId(),
                    "error", "ORDER_ASSIGN_FAILED",
                    "message", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }

        if (order != null) {
            pendingOrderRedisRepository.remove(request.getOrderId());
            Assignment assignment = Assignment.builder()
                    .orderId(request.getOrderId())
                    .courierId(request.getCourierId())
                    .build();
            dispatchDeliveryRecordService.createFromAssignment(order, assignment);
            Long zoneId = order.getZoneId();
            deliveryEventProducer.publishAssigned(DispatchAssignedEvent.builder()
                    .zoneId(zoneId)
                    .orderId(request.getOrderId())
                    .courierId(request.getCourierId())
                    .dispatchMode(DispatchMode.MANUAL)
                    .proposal(false)
                    .build());
            timeoutTracker.trackProposal(
                    request.getOrderId(),
                    request.getCourierId(),
                    Duration.ofSeconds(runtimeDispatchTuningService.responseTimeoutDeadlineSeconds()),
                    order);
            redisTemplate.opsForValue().set(
                    "courier:" + request.getCourierId() + ":lastAssignedAt",
                    Instant.now().toString(),
                    Duration.ofHours(24));
        } else {
            log.warn("manual assign: no local PendingOrder for orderId={}, order updated in order-service only",
                    request.getOrderId());
        }

        return Map.of(
                "success", true,
                "orderId", request.getOrderId(),
                "courierId", request.getCourierId(),
                "mode", "MANUAL_ASSIGN");
    }

    public Map<String, Object> manualBundle(ManualBundleRequest request) {
        String bundleId = "MANUAL-" + UUID.randomUUID();
        String key = MANUAL_BUNDLE_KEY.formatted(bundleId);
        Map<String, Object> data = new HashMap<>();
        data.put("bundleId", bundleId);
        data.put("zoneId", request.getZoneId());
        data.put("orderIds", request.getOrderIds());
        redisTemplate.opsForHash().putAll(key, Map.of(
                "zoneId", String.valueOf(request.getZoneId()),
                "orderIds", request.getOrderIds().toString()
        ));
        redisTemplate.expire(key, Duration.ofHours(2));
        return data;
    }

    public Map<String, Object> updateZoneStatus(Long zoneId, boolean active) {
        return locationServiceClient.updateZoneStatus(zoneId, Map.of("isActive", active));
    }

    private PendingOrder resolvePendingOrderFromOrderService(Long orderId) {
        try {
            List<Map<String, Object>> awaiting = orderServiceClient.getOrdersAwaitingCourier();
            return awaiting.stream()
                    .filter(o -> orderId.equals(asLong(o.get("id"))))
                    .findFirst()
                    .flatMap(pendingOrderEnricher::enrichOptional)
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Unable to resolve order {} from order-service: {}", orderId, ex.getMessage());
            return null;
        }
    }

    private static Long asLong(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (Exception ex) {
            return null;
        }
    }
}
