package com.speedline.delivery.dispatch.scheduler;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.UrgentOrderUnassignedAlertEvent;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class UrgentOrderAlertScheduler {

    private static final String ALERTED_KEY = "dispatch:urgent:alert:%d";

    private final DispatchZoneConfig zoneConfig;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final DeliveryEventProducer deliveryEventProducer;
    private final DispatchMetrics dispatchMetrics;
    private final DispatchProperties properties;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelayString = "${dispatch.urgent.alert-interval-seconds:300}000")
    public void alertUnassignedUrgentOrders() {
        for (var zone : zoneConfig.getActiveZones()) {
            if (zone.getId() == null) {
                continue;
            }
            pendingOrderRedisRepository.findByZone(zone.getId()).stream()
                    .filter(order -> Boolean.TRUE.equals(order.getIsUrgent()))
                    .forEach(this::maybeAlert);
        }
    }

    private void maybeAlert(PendingOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        String key = ALERTED_KEY.formatted(order.getId());
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                Duration.ofSeconds(properties.getUrgent().getAlertIntervalSeconds())
        );
        if (!Boolean.TRUE.equals(isNew)) {
            return;
        }

        long unassignedMinutes = order.getCreatedAt() == null
                ? 0L
                : Duration.between(order.getCreatedAt(), Instant.now()).toMinutes();

        deliveryEventProducer.publishUrgentOrderUnassignedAlert(
                UrgentOrderUnassignedAlertEvent.builder()
                        .orderId(order.getId())
                        .zoneId(order.getZoneId())
                        .partnerId(order.getPartnerId())
                        .orderCreatedAt(order.getCreatedAt())
                        .unassignedMinutes(unassignedMinutes)
                        .build()
        );
        dispatchMetrics.recordUrgentOrderUnassignedAlert(order.getZoneId());
    }
}
