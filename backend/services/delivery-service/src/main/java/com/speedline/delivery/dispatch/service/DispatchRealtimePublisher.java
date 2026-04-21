package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.dto.DispatchCycleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DispatchRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final DispatchDashboardQueryService queryService;

    @Scheduled(fixedDelay = 5000)
    public void publishCourierPositions() {
        messagingTemplate.convertAndSend("/topic/couriers/positions", queryService.getCourierPositions());
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
}
