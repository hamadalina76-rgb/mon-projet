package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.PendingOrderEnricher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerDelayHandler {

    private static final String COURIER_PREFIX = "courier:%d";

    private final PartnerDelayTrackerRepository delayTrackerRepository;
    private final PartnerSlaCounterRepository partnerSlaCounterRepository;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final PendingOrderEnricher pendingOrderEnricher;
    private final CourierResponseTimeoutTracker timeoutTracker;
    private final StringRedisTemplate redisTemplate;
    private final DispatchProperties dispatchProperties;

    public void trackDelay(Map<String, Object> event) {
        if (event == null || event.isEmpty()) {
            return;
        }
        final Long orderId = parseLong(event.get("orderId"));
        if (orderId == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>(event);
        payload.putIfAbsent("delayStartedAt", Instant.now().toString());
        delayTrackerRepository.upsert(payload);
    }

    public void processActiveDelays() {
        final int thresholdMinutes = dispatchProperties.getPartnerDelay().getThresholdMinutes();
        final Instant now = Instant.now();
        for (Map<String, Object> event : delayTrackerRepository.findAll()) {
            final Long orderId = parseLong(event.get("orderId"));
            if (orderId == null) {
                continue;
            }
            final Instant startedAt = parseInstant(event.get("delayStartedAt")).orElse(now);
            final long waitedMinutes = Duration.between(startedAt, now).toMinutes();
            if (waitedMinutes <= thresholdMinutes) {
                continue;
            }
            releaseCourier(parseLong(event.get("courierId")));
            resolvePendingOrder(orderId, event).ifPresent(pendingOrderRedisRepository::add);
            incrementSlaCounter(parseLong(event.get("partnerId")));
            timeoutTracker.removeDeadline(orderId, parseLong(event.get("courierId")));
            timeoutTracker.removeTrackedOrder(orderId);
            delayTrackerRepository.remove(orderId);
        }
    }

    private void incrementSlaCounter(Long partnerId) {
        if (partnerId == null) {
            return;
        }
        long total = partnerSlaCounterRepository.incrementMonthlyDelay(partnerId);
        log.info("Partner SLA delay counter incremented partnerId={} totalMonth={}", partnerId, total);
    }

    private Optional<PendingOrder> resolvePendingOrder(Long orderId, Map<String, Object> event) {
        return timeoutTracker.getTrackedOrder(orderId).or(() -> pendingOrderEnricher.enrichOptional(event));
    }

    private void releaseCourier(Long courierId) {
        if (courierId == null) {
            return;
        }
        final String prefix = COURIER_PREFIX.formatted(courierId);
        redisTemplate.opsForValue().set(prefix + ":status", "IDLE");
        redisTemplate.opsForValue().set(prefix + ":isOnline", "true");
    }

    private static Optional<Instant> parseInstant(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Instant instant) {
            return Optional.of(instant);
        }
        try {
            return Optional.of(Instant.parse(String.valueOf(value)));
        } catch (Exception ex) {
            return Optional.empty();
        }
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
}
