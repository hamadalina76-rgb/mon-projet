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
public class CourierInactivityAlertEvent {

    @Builder.Default
    private String eventType = "COURIER_INACTIVITY_ALERT";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private Long zoneId;
    private Long courierId;
    private long inactiveMinutes;
}
