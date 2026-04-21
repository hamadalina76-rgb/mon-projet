package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.client.LocationServiceClient;
import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.dto.ManualAssignRequest;
import com.speedline.delivery.dispatch.dto.ManualBundleRequest;
import com.speedline.delivery.dispatch.event.PendingOrderEnricher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    public Map<String, Object> manualAssign(ManualAssignRequest request) {
        PendingOrder order = timeoutTracker.getTrackedOrder(request.getOrderId())
                .orElseGet(() -> resolvePendingOrderFromOrderService(request.getOrderId()));
        if (order != null) {
            pendingOrderRedisRepository.remove(request.getOrderId());
            timeoutTracker.trackProposal(
                    request.getOrderId(),
                    request.getCourierId(),
                    Duration.ofSeconds(45),
                    order);
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
