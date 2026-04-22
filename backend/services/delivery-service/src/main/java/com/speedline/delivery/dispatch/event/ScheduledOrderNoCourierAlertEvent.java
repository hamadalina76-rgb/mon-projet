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
public class ScheduledOrderNoCourierAlertEvent {

    @Builder.Default
    private String eventType = "SCHEDULED_ORDER_NO_COURIER_ALERT";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private Long orderId;
    private Long zoneId;
    private Long partnerId;
    private Instant scheduledDeliveryAt;
    private long minutesUntilDelivery;
}
