package com.speedline.delivery.dispatch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DispatchMetrics {

    private final MeterRegistry registry;

    private final Map<Long, AtomicReference<Double>> matchRateByZone = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> pendingOrdersByZone = new ConcurrentHashMap<>();

    public DispatchMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCycle(Long zoneId,
                            int orders,
                            int couriers,
                            int assigned,
                            int unmatched,
                            Duration duration,
                            long pendingOrders) {
        final Tags tags = zoneTags(zoneId);

        Timer.builder("dispatch.cycle.duration")
                .tags(tags)
                .register(registry)
                .record(duration);

        DistributionSummary.builder("dispatch.cycle.orders")
                .tags(tags)
                .register(registry)
                .record(orders);

        DistributionSummary.builder("dispatch.cycle.couriers")
                .tags(tags)
                .register(registry)
                .record(couriers);

        Counter.builder("dispatch.cycle.assigned")
                .tags(tags)
                .register(registry)
                .increment(Math.max(0, assigned));

        Counter.builder("dispatch.cycle.unmatched")
                .tags(tags)
                .register(registry)
                .increment(Math.max(0, unmatched));

        gaugeMatchRate(zoneId).set(orders <= 0 ? 0.0 : ((double) assigned / (double) orders));
        gaugePendingOrders(zoneId).set(Math.max(0L, pendingOrders));
    }

    public void recordError(Long zoneId, String reason) {
        Counter.builder("dispatch.cycle.errors")
                .tags(zoneTags(zoneId).and("reason", reason == null || reason.isBlank() ? "unknown" : reason))
                .register(registry)
                .increment();
    }

    public void recordEligibilityPhase1Only(Long zoneId) {
        Counter.builder("dispatch.eligibility.phase1.only")
                .tags(zoneTags(zoneId))
                .register(registry)
                .increment();
    }

    public void recordEligibilityPhase2Triggered(Long zoneId, String reason) {
        Counter.builder("dispatch.eligibility.phase2.triggered")
                .tags(zoneTags(zoneId).and("reason", reason == null || reason.isBlank() ? "unknown" : reason))
                .register(registry)
                .increment();
    }

    public void recordResponseTimeout(String courierType) {
        Counter.builder("dispatch.response.timeout")
                .tags(Tags.of("courierType", courierType == null || courierType.isBlank() ? "UNKNOWN" : courierType))
                .register(registry)
                .increment();
    }

    public void recordRefusalEscalation(String escalationType) {
        Counter.builder("dispatch.refusal.escalation")
                .tags(Tags.of("escalationType", escalationType == null || escalationType.isBlank() ? "UNKNOWN" : escalationType))
                .register(registry)
                .increment();
    }

    private AtomicReference<Double> gaugeMatchRate(Long zoneId) {
        return matchRateByZone.computeIfAbsent(zoneId, id -> {
            AtomicReference<Double> ref = new AtomicReference<>(0.0d);
            registry.gauge("dispatch.cycle.match_rate", zoneTags(id), ref, AtomicReference::get);
            return ref;
        });
    }

    private AtomicLong gaugePendingOrders(Long zoneId) {
        return pendingOrdersByZone.computeIfAbsent(zoneId, id -> {
            AtomicLong ref = new AtomicLong(0L);
            registry.gauge("dispatch.pending.orders", zoneTags(id), ref, AtomicLong::get);
            return ref;
        });
    }

    private static Tags zoneTags(Long zoneId) {
        return Tags.of("zone", String.valueOf(zoneId));
    }
}
