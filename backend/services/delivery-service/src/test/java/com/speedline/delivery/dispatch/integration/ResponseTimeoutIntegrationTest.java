package com.speedline.delivery.dispatch.integration;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.service.DispatchCycleService;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.dispatch.service.CourierResponseTimeoutTracker;
import com.speedline.delivery.dispatch.scheduler.ResponseTimeoutScheduler;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "eureka.client.enabled=false",
        "dispatch.interval-seconds=60",
        "dispatch.response-timeout.deadline-seconds=1",
        "dispatch.response-timeout.poll-interval-ms=200"
})
@EnabledIfSystemProperty(named = "runDockerTests", matches = "true")
class ResponseTimeoutIntegrationTest {

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
    private PendingOrderRedisRepository pendingOrderRedisRepository;
    @Autowired
    private DispatchCycleService dispatchCycleService;
    @Autowired
    private CourierResponseTimeoutTracker timeoutTracker;
    @Autowired
    private ResponseTimeoutScheduler responseTimeoutScheduler;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private DeliveryEventProducer deliveryEventProducer;

    @AfterEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void assignmentTimeout_requeuesOrderIntoPendingPool() {
        Long zoneId = 7L;
        Long orderId = 9701L;
        Long courierId = 5701L;

        pendingOrderRedisRepository.add(PendingOrder.builder()
                .id(orderId)
                .partnerId(70L)
                .customerId(71L)
                .zoneId(zoneId)
                .partnerLat(36.9)
                .partnerLon(10.21)
                .customerLat(36.91)
                .customerLon(10.22)
                .guaranteedDeliveryMinutes(25)
                .createdAt(Instant.now())
                .build());

        String courierPrefix = "courier:" + courierId;
        redisTemplate.opsForValue().set(courierPrefix + ":isOnline", "true");
        redisTemplate.opsForValue().set(courierPrefix + ":zone", String.valueOf(zoneId));
        redisTemplate.opsForValue().set(courierPrefix + ":type", "INTERNAL");
        redisTemplate.opsForValue().set(courierPrefix + ":status", "IDLE");
        redisTemplate.opsForValue().set(courierPrefix + ":position", "{\"lat\":36.91,\"lng\":10.22}");

        dispatchCycleService.runCycle(zoneId);

        assertEquals(0L, pendingOrderRedisRepository.countByZone(zoneId));

        timeoutTracker.trackProposal(
            orderId,
            courierId,
            java.time.Duration.ofSeconds(1),
            PendingOrder.builder()
                .id(orderId)
                .partnerId(70L)
                .customerId(71L)
                .zoneId(zoneId)
                .partnerLat(36.9)
                .partnerLon(10.21)
                .customerLat(36.91)
                .customerLon(10.22)
                .guaranteedDeliveryMinutes(25)
                .createdAt(Instant.now())
                .build());
        redisTemplate.opsForZSet().add(
            "dispatch:response:deadlines",
            orderId + ":" + courierId,
            Instant.now().minusSeconds(1).toEpochMilli());

        responseTimeoutScheduler.checkTimeouts();

        assertEquals(1L, pendingOrderRedisRepository.countByZone(zoneId));
    }
}
