package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.CourierIncidentAlertEvent;
import com.speedline.delivery.dispatch.event.PendingOrderEnricher;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentHandler {

    private static final String COURIER_PREFIX = "courier:%d";

    private final StringRedisTemplate redisTemplate;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final CourierResponseTimeoutTracker timeoutTracker;
    private final PendingOrderEnricher pendingOrderEnricher;
    private final DeliveryEventProducer deliveryEventProducer;
    private final OrderServiceClient orderServiceClient;

    public void handleIncident(Map<String, Object> event) {
        if (event == null || event.isEmpty()) {
            return;
        }
        final Long orderId = parseLong(event.get("orderId"));
        final Long courierId = parseLong(event.get("courierId"));
        final String incidentType = String.valueOf(event.getOrDefault("incidentType", "UNKNOWN"));
        final boolean damagedProduct = asBoolean(event.get("damagedProduct"));

        markCourierUnavailable(courierId);
        publishAdminAlert(orderId, courierId, incidentType, event);

        if (damagedProduct) {
            processDamagedProduct(orderId, courierId, incidentType, event);
            cleanupTracking(orderId, courierId);
            return;
        }

        resolvePendingOrder(orderId, event).ifPresent(pendingOrderRedisRepository::add);
        cleanupTracking(orderId, courierId);
    }

    private void processDamagedProduct(Long orderId, Long courierId, String incidentType, Map<String, Object> event) {
        if (orderId == null) {
            return;
        }
        try {
            orderServiceClient.cancelOrder(orderId, new OrderServiceClient.CancelOrderRequest(
                    "COURIER",
                    courierId,
                    "AUTO_CANCEL_DAMAGED_PRODUCT:" + incidentType));
        } catch (Exception ex) {
            log.warn("Failed to cancel order {} after incident: {}", orderId, ex.getMessage());
        }

        final BigDecimal refundAmount = resolveRefundAmount(orderId, event);
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Skip refund for order {} because amount is missing/invalid", orderId);
            return;
        }
        try {
            orderServiceClient.refundOrder(orderId, new OrderServiceClient.RefundOrderRequest(refundAmount));
        } catch (Exception ex) {
            log.warn("Failed to refund order {} after incident: {}", orderId, ex.getMessage());
        }
    }

    private BigDecimal resolveRefundAmount(Long orderId, Map<String, Object> event) {
        BigDecimal fromEvent = toBigDecimal(event.get("refundAmount"));
        if (fromEvent != null) {
            return fromEvent;
        }
        if (orderId == null) {
            return null;
        }
        try {
            Map<String, Object> order = orderServiceClient.getOrderById(orderId);
            return toBigDecimal(order.get("total"));
        } catch (Exception ex) {
            log.warn("Failed to resolve refund amount for order {}: {}", orderId, ex.getMessage());
            return null;
        }
    }

    private Optional<PendingOrder> resolvePendingOrder(Long orderId, Map<String, Object> event) {
        Optional<PendingOrder> tracked = timeoutTracker.getTrackedOrder(orderId);
        if (tracked.isPresent()) {
            return tracked;
        }
        return pendingOrderEnricher.enrichOptional(event);
    }

    private void markCourierUnavailable(Long courierId) {
        if (courierId == null) {
            return;
        }
        final String prefix = COURIER_PREFIX.formatted(courierId);
        redisTemplate.opsForValue().set(prefix + ":isOnline", "false");
        redisTemplate.opsForValue().set(prefix + ":status", "IDLE");
    }

    private void publishAdminAlert(Long orderId, Long courierId, String incidentType, Map<String, Object> event) {
        deliveryEventProducer.publishCourierIncidentAlert(
                CourierIncidentAlertEvent.builder()
                        .orderId(orderId)
                        .courierId(courierId)
                        .incidentType(incidentType)
                        .latitude(firstDecimal(event, "gpsLat", "latitude", "lat"))
                        .longitude(firstDecimal(event, "gpsLon", "longitude", "lng"))
                        .message(String.valueOf(event.getOrDefault("message", "Courier incident reported")))
                        .build()
        );
    }

    private void cleanupTracking(Long orderId, Long courierId) {
        timeoutTracker.removeDeadline(orderId, courierId);
        timeoutTracker.removeTrackedOrder(orderId);
    }

    private static boolean asBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private static Double firstDecimal(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Double candidate = toDouble(payload.get(key));
            if (candidate != null) {
                return candidate;
            }
        }
        Object gps = payload.get("gps");
        if (gps instanceof Map<?, ?> gpsMap) {
            for (String key : keys) {
                Double candidate = toDouble(gpsMap.get(key));
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }
}
