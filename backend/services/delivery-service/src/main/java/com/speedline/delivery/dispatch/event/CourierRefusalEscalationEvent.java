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
public class CourierRefusalEscalationEvent {

    @Builder.Default
    private String eventType = "COURIER_REFUSAL_ESCALATION";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private Long orderId;
    private Long courierId;
    private String courierType;
    private String reason;
    private String escalationType;
    private long refusalCount;
}
