package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.dto.DispatchCycleEvent;
import com.speedline.delivery.dispatch.dto.DispatchProposalView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final DispatchDashboardQueryService queryService;

    @Scheduled(fixedDelay = 5000)
    public void publishCourierPositions() {
        try {
            messagingTemplate.convertAndSend("/topic/couriers/positions", queryService.getCourierPositions());
        } catch (Exception ex) {
            log.warn("publishCourierPositions failed: {}", ex.getMessage());
        }
    }

    public void publishZoneCycle(Long zoneId, int orders, int couriers, int assigned, int unmatched) {
        DispatchCycleEvent event = DispatchCycleEvent.builder()
                .zoneId(zoneId)
                .totalOrders(orders)
                .availableCouriers(couriers)
                .assignedOrders(assigned)
                .unmatchedOrders(unmatched)
                .occurredAt(Instant.now())
                .build();
        messagingTemplate.convertAndSend("/topic/zone/" + zoneId + "/cycle", event);
    }

    public void publishZoneStatus(Long zoneId, boolean active) {
        messagingTemplate.convertAndSend("/topic/zone/" + zoneId + "/status",
                Map.of("zoneId", zoneId, "active", active, "occurredAt", Instant.now().toString()));
    }

    /** New or updated dispatch proposal (SEMI_AUTO), for admin UIs. */
    public void publishProposal(DispatchProposalView proposal) {
        if (proposal == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/dispatch/proposals", proposal);
        if (proposal.getZoneId() != null) {
            messagingTemplate.convertAndSend("/topic/zone/" + proposal.getZoneId() + "/proposals", proposal);
        }
    }

    public void publishProposalResolved(Long zoneId, Long orderId, String status) {
        Object payload = Map.of(
                "zoneId", zoneId != null ? zoneId : 0L,
                "orderId", orderId != null ? orderId : 0L,
                "status", status != null ? status : "UNKNOWN",
                "occurredAt", Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/dispatch/proposals/resolved", payload);
        if (zoneId != null) {
            messagingTemplate.convertAndSend("/topic/zone/" + zoneId + "/proposals/resolved", payload);
        }
    }
}
