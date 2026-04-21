package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.CourierRefusalEscalationEvent;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefusalHandler {

    private static final String COURIER_TYPE_KEY = "courier:%d:type";

    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final RefusalCounterRepository refusalCounterRepository;
    private final CourierResponseTimeoutTracker timeoutTracker;
    private final DeliveryEventProducer deliveryEventProducer;
    private final DispatchProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final DispatchMetrics dispatchMetrics;

    public void handleRefusal(Long orderId, Long courierId, String reason) {
        if (orderId == null || courierId == null) return;

        timeoutTracker.getTrackedOrder(orderId).ifPresent(pendingOrderRedisRepository::add);

        long refusalCount = refusalCounterRepository.incrementDailyRefusal(courierId);
        refusalCounterRepository.blacklistOrderForCourier(orderId, courierId);

        CourierType courierType = resolveCourierType(courierId);
        maybePublishEscalation(orderId, courierId, reason, refusalCount, courierType);

        timeoutTracker.removeDeadline(orderId, courierId);
        timeoutTracker.removeTrackedOrder(orderId);
        if ("TIMEOUT".equalsIgnoreCase(reason)) {
            dispatchMetrics.recordResponseTimeout(courierType.name());
        }
    }

    private void maybePublishEscalation(Long orderId,
                                        Long courierId,
                                        String reason,
                                        long refusalCount,
                                        CourierType type) {
        String escalationType = null;
        if (type == CourierType.INTERNAL) {
            if (refusalCount >= properties.getRefusal().getInternalHrThreshold()) {
                escalationType = "HR";
            } else if (refusalCount >= properties.getRefusal().getInternalWarningThreshold()) {
                escalationType = "WARNING";
            }
        } else {
            if (refusalCount >= properties.getRefusal().getExternalScoreDegradationThreshold()) {
                escalationType = "SCORE_DEGRADATION";
            }
        }

        if (escalationType == null) {
            return;
        }

        deliveryEventProducer.publishRefusalEscalation(CourierRefusalEscalationEvent.builder()
                .orderId(orderId)
                .courierId(courierId)
                .courierType(type.name())
                .reason(reason)
                .refusalCount(refusalCount)
                .escalationType(escalationType)
                .build());
        dispatchMetrics.recordRefusalEscalation(escalationType);
    }

    private CourierType resolveCourierType(Long courierId) {
        String raw = redisTemplate.opsForValue().get(String.format(COURIER_TYPE_KEY, courierId));
        if (raw == null || raw.isBlank()) return CourierType.INTERNAL;
        try {
            return CourierType.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            log.debug("Unknown courier type for {}: {}", courierId, raw);
            return CourierType.INTERNAL;
        }
    }
}
