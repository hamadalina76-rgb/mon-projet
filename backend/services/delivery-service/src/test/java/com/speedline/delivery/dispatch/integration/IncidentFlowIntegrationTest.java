package com.speedline.delivery.dispatch.integration;

import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.service.CourierResponseTimeoutTracker;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "eureka.client.enabled=false",
        "dispatch.interval-seconds=60"
})
@EnabledIfSystemProperty(named = "runDockerTests", matches = "true")
@SuppressWarnings("resource")
class IncidentFlowIntegrationTest {

    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @BeforeAll
    static void initContainer() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Docker integration test.");
        REDIS.start();
    }

    @AfterAll
    static void stopContainer() {
        REDIS.stop();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    @Qualifier("deliveryIncidentMessageHandler")
    private MessageHandler deliveryIncidentMessageHandler;
    @Autowired
    private PendingOrderRedisRepository pendingOrderRedisRepository;
    @Autowired
    private CourierResponseTimeoutTracker timeoutTracker;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private DeliveryEventProducer deliveryEventProducer;
    @MockBean
    private OrderServiceClient orderServiceClient;

    @AfterEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void incidentWithoutDamage_requeuesOrderAndAlertsAdmin() {
        Long orderId = 881L;
        Long courierId = 771L;
        Long zoneId = 5L;
        timeoutTracker.trackProposal(
                orderId,
                courierId,
                java.time.Duration.ofSeconds(45),
                PendingOrder.builder()
                        .id(orderId)
                        .partnerId(10L)
                        .customerId(20L)
                        .zoneId(zoneId)
                        .partnerLat(36.9)
                        .partnerLon(10.2)
                        .customerLat(36.91)
                        .customerLon(10.21)
                        .createdAt(Instant.now())
                        .build()
        );

        String payload = "{" +
                "\"eventType\":\"DELIVERY_INCIDENT\"," +
                "\"orderId\":" + orderId + "," +
                "\"courierId\":" + courierId + "," +
                "\"incidentType\":\"BREAKDOWN\"," +
                "\"damagedProduct\":false," +
                "\"gpsLat\":36.8," +
                "\"gpsLon\":10.1}";
        deliveryIncidentMessageHandler.handleMessage(MessageBuilder.withPayload(payload).build());

        assertEquals(1L, pendingOrderRedisRepository.countByZone(zoneId));
        assertEquals("false", redisTemplate.opsForValue().get("courier:" + courierId + ":isOnline"));
        verify(deliveryEventProducer).publishCourierIncidentAlert(any());
    }

    @Test
    void incidentWithDamagedProduct_triggersCancelAndRefund() {
        Long orderId = 882L;
        when(orderServiceClient.getOrderById(orderId)).thenReturn(Map.of("total", 30.0));

        String payload = "{" +
                "\"eventType\":\"DELIVERY_INCIDENT\"," +
                "\"orderId\":" + orderId + "," +
                "\"courierId\":772," +
                "\"incidentType\":\"ACCIDENT\"," +
                "\"damagedProduct\":true}";
        deliveryIncidentMessageHandler.handleMessage(MessageBuilder.withPayload(payload).build());

        verify(orderServiceClient).cancelOrder(eq(orderId), any(OrderServiceClient.CancelOrderRequest.class));
        verify(orderServiceClient).refundOrder(eq(orderId), any(OrderServiceClient.RefundOrderRequest.class));
    }
}
