package com.speedline.delivery.dispatch.scheduler;

import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import com.speedline.delivery.dispatch.event.CourierInactivityAlertEvent;
import com.speedline.delivery.dispatch.service.CourierAvailabilityService;
import com.speedline.delivery.dispatch.service.EligibilityFilter;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactivityMonitorScheduler {

    private static final String LAST_ASSIGNED_KEY = "courier:%d:lastAssignedAt";
    private static final String SHIFT_STARTED_KEY = "courier:%d:shiftStartedAt";
    private static final String ALERTED_KEY = "courier:%d:inactivity:alerted";

    private final DispatchZoneConfig zoneConfig;
    private final DispatchProperties dispatchProperties;
    private final CourierAvailabilityService courierAvailabilityService;
    private final StringRedisTemplate redisTemplate;
    private final DeliveryEventProducer deliveryEventProducer;

    @Scheduled(fixedDelayString = "${dispatch.inactivity.scheduler-interval-seconds:60}000")
    public void checkInactiveInternalCouriers() {
        int thresholdMinutes = dispatchProperties.getInactivity().getThresholdMinutes();
        for (var zone : zoneConfig.getActiveZones()) {
            if (zone.getId() == null) continue;

            for (AvailableCourier courier : courierAvailabilityService.findOnlineByZone(zone.getId())) {
                if (courier.getType() != CourierType.INTERNAL || courier.getStatus() != CourierStatus.IDLE) {
                    continue;
                }
                if (!EligibilityFilter.isInShift(courier, LocalTime.now())) {
                    continue;
                }
                Long courierId = courier.getId();
                if (courierId == null) continue;

                Instant base = readInstant(String.format(LAST_ASSIGNED_KEY, courierId));
                if (base == null) {
                    base = readInstant(String.format(SHIFT_STARTED_KEY, courierId));
                }
                if (base == null) continue;

                long inactiveMin = Duration.between(base, Instant.now()).toMinutes();
                if (inactiveMin <= thresholdMinutes) continue;

                String alertKey = String.format(ALERTED_KEY, courierId);
                if (Boolean.TRUE.equals(redisTemplate.hasKey(alertKey))) {
                    continue;
                }

                deliveryEventProducer.publishInactivityAlert(CourierInactivityAlertEvent.builder()
                        .zoneId(zone.getId())
                        .courierId(courierId)
                        .inactiveMinutes(inactiveMin)
                        .build());

                redisTemplate.opsForValue().set(alertKey, "1", Duration.ofMinutes(thresholdMinutes));
            }
        }
    }

    private Instant readInstant(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null || raw.isBlank()) return null;
            return Instant.parse(raw);
        } catch (Exception ex) {
            log.debug("Invalid instant key {}: {}", key, ex.getMessage());
            return null;
        }
    }
}
