package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.PendingOrderEnricher;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidentHandlerTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private PendingOrderRedisRepository pendingOrderRedisRepository;
    @Mock
    private CourierResponseTimeoutTracker timeoutTracker;
    @Mock
    private PendingOrderEnricher pendingOrderEnricher;
    @Mock
    private DeliveryEventProducer deliveryEventProducer;
    @Mock
    private OrderServiceClient orderServiceClient;

    private IncidentHandler incidentHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        incidentHandler = new IncidentHandler(
                redisTemplate,
                pendingOrderRedisRepository,
                timeoutTracker,
                pendingOrderEnricher,
                deliveryEventProducer,
                orderServiceClient
        );
    }

    @Test
    void incidentWithoutDamage_requeuesOrderAndAlertsAdmin() {
        PendingOrder pendingOrder = PendingOrder.builder().id(11L).zoneId(1L).build();
        when(timeoutTracker.getTrackedOrder(11L)).thenReturn(Optional.of(pendingOrder));

        incidentHandler.handleIncident(Map.of(
                "eventType", "DELIVERY_INCIDENT",
                "orderId", 11L,
                "courierId", 77L,
                "incidentType", "PUNCTURE",
                "damagedProduct", false,
                "gpsLat", 36.8,
                "gpsLon", 10.1
        ));

        verify(valueOperations).set("courier:77:isOnline", "false");
        verify(pendingOrderRedisRepository).add(pendingOrder);
        verify(deliveryEventProducer).publishCourierIncidentAlert(any());
        verify(orderServiceClient, never()).refundOrder(any(), any());
    }

    @Test
    void incidentWithDamagedProduct_cancelsAndRefundsOrder() {
        when(timeoutTracker.getTrackedOrder(22L)).thenReturn(Optional.empty());
        when(orderServiceClient.getOrderById(22L)).thenReturn(Map.of("total", new BigDecimal("42.50")));

        incidentHandler.handleIncident(Map.of(
                "eventType", "DELIVERY_INCIDENT",
                "orderId", 22L,
                "courierId", 88L,
                "incidentType", "ACCIDENT",
                "damagedProduct", true
        ));

        verify(orderServiceClient).cancelOrder(eq(22L), any(OrderServiceClient.CancelOrderRequest.class));
        verify(orderServiceClient).refundOrder(eq(22L), any(OrderServiceClient.RefundOrderRequest.class));
        verify(pendingOrderRedisRepository, never()).add(any());
    }
}
