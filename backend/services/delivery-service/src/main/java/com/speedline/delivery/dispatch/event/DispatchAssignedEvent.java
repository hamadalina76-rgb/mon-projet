package com.speedline.delivery.dispatch.event;

import com.speedline.delivery.dispatch.config.DispatchMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DISP-101: event published on topic {@code dispatch-events} when the dispatch loop
 * matches a pending order with a courier. Downstream consumers (Delivery JPA writer,
 * notification-service, WebSocket broadcaster) use {@code eventId} for idempotency.
 *
 * <p>For zones in {@link DispatchMode#SEMI_AUTO}, {@link #proposal} is {@code true}:
 * the assignment awaits operator confirmation before being acted upon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchAssignedEvent {

    @Builder.Default
    private String eventType = "DISPATCH_ASSIGNED";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private Long zoneId;
    private Long orderId;
    private Long courierId;
    private Long bundleId;

    private Double cost;
    private Integer etaPickupMin;
    private Integer etaDeliveryMin;

    private DispatchMode dispatchMode;

    /** {@code true} when produced for a SEMI_AUTO zone — requires operator confirmation. */
    private boolean proposal;
}
