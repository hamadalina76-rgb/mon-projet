package com.speedline.delivery.dispatch.config.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.speedline.delivery.matching.cost.model.CostComponentKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DISP-205 request/response DTOs for grouped dispatch configuration.
 */
public final class DispatchConfigDtos {

    private DispatchConfigDtos() {
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetaResponse {
        private long activeVersion;
        private int optimisticLock;
        private long redisConfigVersion;
    }

    @Data
    public static class GeneralConfigDto {
        private Integer intervalSeconds;
        private Integer responseTimeoutSeconds;
        private Integer lockTtlSeconds;
        private Integer preAssignmentFinishWindowSeconds;
        private Double preAssignmentCostPenalty;
        private Integer eligibilityOrderWaitingThresholdSeconds;
        private Integer eligibilityInternalShortageThreshold;
    }

    @Data
    public static class ScoringComponentDto {
        @NotNull
        private CostComponentKey key;
        private boolean enabled = true;
        @Min(0)
        @Max(10)
        private int weight = 10;
        @Min(0)
        @Max(99)
        private int order;
        private boolean mandatory;
    }

    @Data
    public static class ScoringConfigDto {
        @NotNull
        @Valid
        private List<ScoringComponentDto> components;
        /** Legacy-compatible array (same shape as Redis {@code dispatch:cost:components}). */
        private List<ScoringComponentDto> legacyComponents;
    }

    @Data
    public static class InternalExternalConfigDto {
        private Boolean internalPriorityOverExternal;
        private Integer externalScoreDegradationThreshold;
        private Integer partnerDelayThresholdMinutes;
        private Integer partnerDelaySchedulerIntervalSeconds;
        private String shiftWindowStartLocal;
        private String shiftWindowEndLocal;
    }

    @Data
    public static class BundlingConfigDto {
        private Boolean enabled;
        private Integer maxBundleSize;
        private Integer dropoffRadiusMeters;
        private Integer merchantRadiusMeters;
        private Integer timeWindowSeconds;
        private Boolean internalOnly;
        private Double averageSpeedKmh;
        private Boolean respectUrgentFlag;
        private String assignmentStrategy;
        private Boolean compensationEnabled;
        private Integer compensationThresholdMinutes;
        private Double compensationPercentOfTotal;
        private String compensationCurrency;
    }

    @Data
    public static class ExclusivityCellDto {
        @NotNull
        private Long zoneId;
        @NotNull
        private String commerceType;
        private boolean allowed = true;
    }

    @Data
    public static class ExclusivityPartnerOverrideDto {
        @NotNull
        private Long partnerId;
        @NotNull
        private Long zoneId;
        @NotNull
        private String commerceType;
        private boolean allowed;
    }

    @Data
    public static class ExclusivityConfigDto {
        private List<ExclusivityCellDto> cells;
        private List<ExclusivityPartnerOverrideDto> partnerOverrides;
    }

    @Data
    public static class GroupEnvelope<T> {
        private MetaResponse meta;
        private T data;
    }

    @Data
    public static class SimulateRequest {
        private Long zoneId;
        @Valid
        private ScoringConfigDto scoringOverlay;
        @Valid
        private GeneralConfigDto generalOverlay;
    }

    @Data
    @Builder
    public static class SimulatePairResultDto {
        private long orderId;
        private long courierId;
        private double totalCost;
        private boolean eliminated;
        private String eliminationReason;
        private List<ComponentScoreDto> components;
    }

    @Data
    @Builder
    public static class ComponentScoreDto {
        private CostComponentKey key;
        private double weightedCost;
        private boolean eliminated;
        private String detail;
    }

    @Data
    @Builder
    public static class SimulateResponse {
        private long simulationRunId;
        private String status;
        private List<SimulatePairResultDto> pairResults;
        private String note;
    }

    @Data
    @Builder
    public static class ReplayResponse {
        private String cycleId;
        private long replaySessionId;
        private List<SimulatePairResultDto> pairResults;
    }

    @Data
    public static class GeneralPutRequest {
        @NotNull
        private Long baseVersion;
        @NotNull
        @Valid
        private GeneralConfigDto data;
    }

    @Data
    public static class ScoringPutRequest {
        @NotNull
        private Long baseVersion;
        @NotNull
        @Valid
        private ScoringConfigDto data;
    }

    @Data
    public static class InternalExternalPutRequest {
        @NotNull
        private Long baseVersion;
        @NotNull
        @Valid
        private InternalExternalConfigDto data;
    }

    @Data
    public static class BundlingPutRequest {
        @NotNull
        private Long baseVersion;
        @NotNull
        @Valid
        private BundlingConfigDto data;
    }

    @Data
    public static class ExclusivityPutRequest {
        @NotNull
        private Long baseVersion;
        @NotNull
        @Valid
        private ExclusivityConfigDto data;
    }
}
