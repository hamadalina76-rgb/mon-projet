package com.speedline.delivery.dispatch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatchRealtimePublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private DispatchDashboardQueryService queryService;

    private DispatchRealtimePublisher publisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        publisher = new DispatchRealtimePublisher(messagingTemplate, queryService);
    }

    @Test
    void publishesCourierPositionsEveryTick() {
        when(queryService.getCourierPositions()).thenReturn(List.of());
        publisher.publishCourierPositions();
        verify(messagingTemplate).convertAndSend("/topic/couriers/positions", List.of());
    }

    @Test
    void publishesZoneCycleEvent() {
        publisher.publishZoneCycle(1L, 10, 5, 4, 6);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/zone/1/cycle"), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void publishesZoneStatusEvent() {
        publisher.publishZoneStatus(7L, true);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/zone/7/status"), org.mockito.ArgumentMatchers.any(Object.class));
    }
}
