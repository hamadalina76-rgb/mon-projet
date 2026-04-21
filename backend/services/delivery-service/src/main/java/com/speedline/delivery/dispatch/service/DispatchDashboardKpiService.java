package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.dto.DispatchDashboardKpisResponse;
import com.speedline.delivery.domain.DeliveryStatus;
import com.speedline.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DispatchDashboardKpiService {

    private final DeliveryRepository deliveryRepository;
    private final StringRedisTemplate redisTemplate;

    public DispatchDashboardKpisResponse getGlobalKpis() {
        long pending = readLong("dispatch:kpi:pending:total");
        long assignedFirstCycle = readLong("dispatch:kpi:first-cycle:assigned");
        double firstCycleRate = pending > 0 ? percentage(assignedFirstCycle, pending) : 0d;

        LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
        long deliveredLastHour = Objects.requireNonNullElse(
                deliveryRepository.countByStatusAndDeliveredAtAfter(DeliveryStatus.DELIVERED, lastHour), 0L);
        long activeCouriers = Math.max(1L, readLong("dispatch:kpi:active-couriers:last-hour"));
        double deliveriesPerCourierPerHour = (double) deliveredLastHour / activeCouriers;

        long totalCompleted = Objects.requireNonNullElse(deliveryRepository.countByDeliveredAtIsNotNull(), 0L);
        long failed = Objects.requireNonNullElse(deliveryRepository.countByStatus(DeliveryStatus.FAILED), 0L);
        double failureRate = totalCompleted > 0 ? percentage(failed, totalCompleted) : 0d;

        long onTime = readLong("dispatch:kpi:on-time:delivered");
        long delivered = Math.max(1L, readLong("dispatch:kpi:delivered:total"));
        double onTimeRate = percentage(onTime, delivered);

        long bundled = readLong("dispatch:kpi:bundled:assigned");
        long totalAssigned = Math.max(1L, readLong("dispatch:kpi:assigned:total"));
        double bundlingRate = percentage(bundled, totalAssigned);

        double avgAssignmentDelay = readDouble("dispatch:kpi:assignment-delay:avg-seconds");

        return DispatchDashboardKpisResponse.builder()
                .firstCycleDispatchRate(round2(firstCycleRate))
                .averageAssignmentDelaySeconds(round2(avgAssignmentDelay))
                .deliveriesPerCourierPerHour(round2(deliveriesPerCourierPerHour))
                .bundlingRate(round2(bundlingRate))
                .failureRate(round2(failureRate))
                .onTimeRate(round2(onTimeRate))
                .build();
    }

    public void updateAssignmentDelayMetric(Duration delay) {
        if (delay == null || delay.isNegative()) return;
        redisTemplate.opsForValue().set("dispatch:kpi:assignment-delay:avg-seconds",
                String.valueOf(delay.toSeconds()));
    }

    private long readLong(String key) {
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) return 0L;
        try {
            return Long.parseLong(raw);
        } catch (Exception ex) {
            return 0L;
        }
    }

    private double readDouble(String key) {
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) return 0d;
        try {
            return Double.parseDouble(raw);
        } catch (Exception ex) {
            return 0d;
        }
    }

    private static double percentage(long part, long total) {
        if (total <= 0) return 0d;
        return ((double) part * 100d) / (double) total;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
