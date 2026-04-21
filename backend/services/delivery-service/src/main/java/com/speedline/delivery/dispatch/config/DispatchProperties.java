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
    private DeliveryIncident deliveryIncident = new DeliveryIncident();
    private PartnerDelay partnerDelay = new PartnerDelay();
    private AdminAlerts adminAlerts = new AdminAlerts();
    private Matching matching = new Matching();

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
        private int orderWaitingThresholdSeconds;
        private int internalShortageThreshold;
    }

    @Data
    public static class PreAssignment {
        private int finishWindowSeconds;
        private double costPenalty;
    }

    @Data
    public static class Refusal {
        private int internalWarningThreshold;
        private int internalHrThreshold;
        private int externalScoreDegradationThreshold;
        private int counterTtlHours;
        private int blacklistTtlSeconds;
    }

    @Data
    public static class ResponseTimeout {
        private int deadlineSeconds;
        private int pollIntervalMs;
    }

    @Data
    public static class Inactivity {
        private int schedulerIntervalSeconds;
        private int thresholdMinutes;
    }

    @Data
    public static class CourierResponse {
        private String refusalSubscription;
    }

    @Data
    public static class DeliveryIncident {
        private String subscription;
    }

    @Data
    public static class PartnerDelay {
        private String subscription;
        private int schedulerIntervalSeconds;
        private int thresholdMinutes;
    }

    @Data
    public static class AdminAlerts {
        private String topic;
        private String hrTopic;
        private String reliabilityTopic;
    }

    @Data
    public static class Matching {
        private double infiniteCost = 1_000_000.0;
        private double availabilityUnavailablePenalty = 80.0;
        private double availabilityOnMissionPenalty = 20.0;
        private double performanceMissingRatingPenalty = 10.0;
        private double performanceBelowThreshold = 4.0;
        private double performancePenaltyPerPoint = 30.0;
        private double routeAlignmentBonusFactor = 20.0;
        private Eta eta = new Eta();

        @Data
        public static class Eta {
            private double defaultSpeedKmh = 20.0;
            private double motorcycleSpeedKmh = 25.0;
            private double bikeSpeedKmh = 15.0;
            private double motorTricycleSpeedKmh = 22.0;
            private double carSpeedKmh = 28.0;
        }
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
