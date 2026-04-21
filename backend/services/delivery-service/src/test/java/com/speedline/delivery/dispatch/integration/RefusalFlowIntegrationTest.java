package com.speedline.delivery.dispatch.integration;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.CourierRefusalEscalationEvent;
import com.speedline.delivery.dispatch.service.CourierResponseTimeoutTracker;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.dispatch.service.RefusalCounterRepository;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentCaptor;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "eureka.client.enabled=false",
        "dispatch.interval-seconds=60"
})
@EnabledIfSystemProperty(named = "runDockerTests", matches = "true")
class RefusalFlowIntegrationTest {

    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @BeforeAll
    static void initContainer() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Docker integration test.");
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    @Qualifier("courierResponseMessageHandler")
    private MessageHandler courierResponseMessageHandler;
    @Autowired
    private CourierResponseTimeoutTracker timeoutTracker;
    @Autowired
    private PendingOrderRedisRepository pendingOrderRedisRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private DeliveryEventProducer deliveryEventProducer;

    @AfterEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void threeInternalRefusals_publishThreeEscalations_andCounterHasDailyTtl() {
        Long orderId = 9301L;
        Long courierId = 4301L;

        pendingOrderRedisRepository.add(PendingOrder.builder()
                .id(orderId)
                .partnerId(15L)
                .customerId(16L)
                .zoneId(1L)
                .partnerLat(36.8)
                .partnerLon(10.1)
                .customerLat(36.81)
                .customerLon(10.12)
                .guaranteedDeliveryMinutes(30)
                .createdAt(Instant.now())
                .build());

        redisTemplate.opsForValue().set("courier:" + courierId + ":type", "INTERNAL");

        timeoutTracker.trackProposal(
                orderId,
                courierId,
                java.time.Duration.ofSeconds(45),
                PendingOrder.builder()
                        .id(orderId)
                        .partnerId(15L)
                        .customerId(16L)
                        .zoneId(1L)
                        .partnerLat(36.8)
                        .partnerLon(10.1)
                        .customerLat(36.81)
                        .customerLon(10.12)
                        .guaranteedDeliveryMinutes(30)
                        .createdAt(Instant.now())
                        .build());

        for (int i = 0; i < 3; i++) {
            String payload = "{" +
                    "\"eventType\":\"COURIER_ORDER_REFUSED\"," +
                    "\"orderId\":" + orderId + "," +
                    "\"courierId\":" + courierId + "," +
                    "\"reason\":\"TOO_FAR\"}";
            courierResponseMessageHandler.handleMessage(MessageBuilder.withPayload(payload).build());
        }

        ArgumentCaptor<CourierRefusalEscalationEvent> escalationCaptor =
                ArgumentCaptor.forClass(CourierRefusalEscalationEvent.class);
        verify(deliveryEventProducer).publishRefusalEscalation(escalationCaptor.capture());
        verify(deliveryEventProducer).publishRefusalEscalation(escalationCaptor.capture());

        List<CourierRefusalEscalationEvent> events = escalationCaptor.getAllValues();
        assertEquals("WARNING", events.get(0).getEscalationType());
        assertEquals("HR", events.get(1).getEscalationType());

        String counterKey = RefusalCounterRepository.dailyRefusalKey(courierId);
        String counter = redisTemplate.opsForValue().get(counterKey);
        assertEquals("3", counter);

        Long ttlSeconds = redisTemplate.getExpire(counterKey);
        assertNotNull(ttlSeconds);
        assertTrue(ttlSeconds > 0L && ttlSeconds <= 24 * 3600L,
                "Daily refusal key should have a 24h TTL window");
    }
}
