package com.speedline.delivery.dispatch.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DispatchCycleEvent {
    private Long zoneId;
    private int totalOrders;
    private int availableCouriers;
    private int assignedOrders;
    private int unmatchedOrders;
    private Instant occurredAt;
}
