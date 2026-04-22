package com.speedline.delivery.dispatch.dto;

import com.speedline.delivery.dispatch.config.DispatchMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DispatchZoneMetricsResponse {
    private Long zoneId;
    /** Nom affiché (location-service), si disponible */
    private String zoneName;
    /** Statut actif/inactif depuis location-service */
    private Boolean zoneActive;
    /** Effective dispatch mode (YAML + optional Redis override). */
    private DispatchMode mode;
    /** True when mode is overridden in Redis. */
    private boolean runtimeModeOverride;
    private long onlineCouriers;
    private long idleCouriers;
    private long onDeliveryCouriers;
    private long pendingOrders;
    private double averageAssignmentDelaySeconds;
}
