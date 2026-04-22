package com.speedline.delivery.dispatch.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrgentOrderUnassignedAlertEvent {

    @Builder.Default
    private String eventType = "URGENT_ORDER_UNASSIGNED_ALERT";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private Long orderId;
    private Long zoneId;
    private Long partnerId;
    private Instant orderCreatedAt;
    private long unassignedMinutes;
}
