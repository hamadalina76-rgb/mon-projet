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
    private Solver solver = new Solver();
    private Eligibility eligibility = new Eligibility();
    private PreAssignment preAssignment = new PreAssignment();
    private Refusal refusal = new Refusal();
    private ResponseTimeout responseTimeout = new ResponseTimeout();
    private Inactivity inactivity = new Inactivity();
    private CourierResponse courierResponse = new CourierResponse();
    private AdminAlerts adminAlerts = new AdminAlerts();

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

    /**
     * DISP-102: Solver selection + OR-Tools microservice settings.
     */
    @Data
    public static class Solver {
        /** Maximum order count routed to GreedySolver (inclusive). */
        private int greedyMaxOrders;
        /** Maximum order count routed to HungarianSolver (inclusive, above greedy threshold). */
        private int hungarianMaxOrders;
        /** Sentinel value used to represent infeasible pairs in padded matrices. */
        private double infCostPlaceholder;

        private OrTools ortools = new OrTools();

        @Data
        public static class OrTools {
            private boolean enabled;
            private String url;
            private int timeoutMs;
        }
    }

    @Data
    public static class Eligibility {
        private int orderWaitingThresholdSeconds = 180;
        private int internalShortageThreshold = 2;
    }

    @Data
    public static class PreAssignment {
        private int finishWindowSeconds = 180;
        private double costPenalty = 1.2;
    }

    @Data
    public static class Refusal {
        private int internalWarningThreshold = 2;
        private int internalHrThreshold = 3;
        private int externalScoreDegradationThreshold = 5;
        private int counterTtlHours = 24;
        private int blacklistTtlSeconds = 300;
    }

    @Data
    public static class ResponseTimeout {
        private int deadlineSeconds = 45;
        private int pollIntervalMs = 5000;
    }

    @Data
    public static class Inactivity {
        private int schedulerIntervalSeconds = 60;
        private int thresholdMinutes = 15;
    }

    @Data
    public static class CourierResponse {
        private String refusalSubscription;
    }

    @Data
    public static class AdminAlerts {
        private String topic;
        private String hrTopic;
        private String reliabilityTopic;
    }

    @Data
    public static class ZoneConfig {
        private Long id;
        private DispatchMode mode = DispatchMode.AUTO;
        private int maxCapacity = 50;
        private Integer internalShortageThreshold;
        /** Optional per-zone override; 0 or negative = use global {@link DispatchProperties#intervalSeconds}. */
        private int intervalOverrideSeconds = 0;
    }
}
