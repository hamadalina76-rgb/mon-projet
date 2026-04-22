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
public class PartnerPreparationStartEvent {

    @Builder.Default
    private String eventType = "PARTNER_PREPARATION_START";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private Long orderId;
    private Long partnerId;
    private Long zoneId;
    private Instant scheduledDeliveryAt;
    private Instant injectedAt;
}
