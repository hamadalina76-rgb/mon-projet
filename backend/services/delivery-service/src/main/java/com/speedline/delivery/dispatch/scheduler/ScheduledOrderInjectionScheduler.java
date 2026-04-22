package com.speedline.delivery.dispatch.scheduler;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.event.PartnerPreparationStartEvent;
import com.speedline.delivery.dispatch.event.ScheduledOrderNoCourierAlertEvent;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.dispatch.service.CourierAvailabilityService;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.dispatch.service.ScheduledOrderRedisRepository;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledOrderInjectionScheduler {

    private static final String NO_COURIER_ALERT_KEY = "dispatch:scheduled:nocourier:alert:%d";

    private final DispatchProperties properties;
    private final DispatchZoneConfig zoneConfig;
    private final ScheduledOrderRedisRepository scheduledOrderRepository;
    private final PendingOrderRedisRepository pendingOrderRepository;
    private final CourierAvailabilityService courierAvailabilityService;
    private final DeliveryEventProducer deliveryEventProducer;
    private final DispatchMetrics dispatchMetrics;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelayString = "${dispatch.scheduled-orders.injection-interval-seconds:60}000")
    public void run() {
        Instant now = Instant.now();
        long leadSeconds = properties.getScheduledOrders().getLeadTimeMinutes() * 60L;
        long alertLeadSeconds = properties.getScheduledOrders().getNoCourierAlertLeadMinutes() * 60L;
        long alertWindowSeconds = properties.getScheduledOrders().getNoCourierAlertWindowMinutes() * 60L;

        for (var zone : zoneConfig.getActiveZones()) {
            if (zone.getId() == null) {
                continue;
            }
            Long zoneId = zone.getId();

            scheduledOrderRepository.findDueWithin(zoneId, now, leadSeconds).forEach(order -> {
                pendingOrderRepository.add(order);
                scheduledOrderRepository.remove(order.getId());
                deliveryEventProducer.publishPartnerPreparationStart(
                        PartnerPreparationStartEvent.builder()
                                .orderId(order.getId())
                                .partnerId(order.getPartnerId())
                                .zoneId(zoneId)
                                .scheduledDeliveryAt(order.getScheduledDeliveryAt())
                                .injectedAt(now)
                                .build());
                dispatchMetrics.recordScheduledOrderInjected(zoneId);
                log.info("Injected scheduled order orderId={} zoneId={}", order.getId(), zoneId);
            });

            long alertStart = now.plusSeconds(alertLeadSeconds).toEpochMilli();
            long alertEnd = now.plusSeconds(alertLeadSeconds + alertWindowSeconds).toEpochMilli();
            scheduledOrderRepository.findByScoreRange(zoneId, alertStart, alertEnd).forEach(order -> {
                if (order.getId() == null) {
                    return;
                }
                String key = NO_COURIER_ALERT_KEY.formatted(order.getId());
                Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(2));
                if (!Boolean.TRUE.equals(isNew)) {
                    return;
                }

                List<AvailableCourier> couriers = courierAvailabilityService.findOnlineByZone(zoneId);
                if (couriers.isEmpty()) {
                    long minutesUntilDelivery = order.getScheduledDeliveryAt() == null
                            ? 0L
                            : Duration.between(now, order.getScheduledDeliveryAt()).toMinutes();
                    deliveryEventProducer.publishScheduledOrderNoCourierAlert(
                            ScheduledOrderNoCourierAlertEvent.builder()
                                    .orderId(order.getId())
                                    .zoneId(zoneId)
                                    .partnerId(order.getPartnerId())
                                    .scheduledDeliveryAt(order.getScheduledDeliveryAt())
                                    .minutesUntilDelivery(minutesUntilDelivery)
                                    .build());
                    dispatchMetrics.recordScheduledNoCourierAlert(zoneId);
                }
            });
        }
    }
}
