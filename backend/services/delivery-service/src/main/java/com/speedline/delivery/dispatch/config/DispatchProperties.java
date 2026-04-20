package com.speedline.delivery.dispatch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * DISP-101: bound to the {@code dispatch.*} block in application.yml.
 */
@Data
@ConfigurationProperties(prefix = "dispatch")
public class DispatchProperties {

    /** Cycle interval in seconds (configured via dispatch.interval-seconds). */
    private int intervalSeconds;

    private Scheduler scheduler = new Scheduler();
    private Events events = new Events();
    private OrderEvents orderEvents = new OrderEvents();
    private Lock lock = new Lock();
    private Pending pending = new Pending();
    private Enricher enricher = new Enricher();

    private List<ZoneConfig> zones = new ArrayList<>();

    @Data
    public static class Scheduler {
        private int poolSize;
    }

    @Data
    public static class Events {
        private String topic;
    }

    @Data
    public static class OrderEvents {
        private String subscription;
    }

    @Data
    public static class Lock {
        private int ttlSeconds;
    }

    @Data
    public static class Pending {
        private int ttlHours;
    }

    @Data
    public static class Enricher {
        private long defaultZoneId;
        private int defaultGuaranteedDeliveryMinutes;
    }

    @Data
    public static class ZoneConfig {
        private Long id;
        private DispatchMode mode = DispatchMode.AUTO;
        private int maxCapacity = 50;
        /** Optional per-zone override; 0 or negative = use global {@link DispatchProperties#intervalSeconds}. */
        private int intervalOverrideSeconds = 0;
    }
}
