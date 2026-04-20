package com.speedline.delivery.dispatch.integration;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.service.DispatchCycleService;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
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

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "eureka.client.enabled=false",
        "dispatch.interval-seconds=60"
})
@EnabledIfSystemProperty(named = "runDockerTests", matches = "true")
class DispatchCycleIntegrationTest {

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
    private StringRedisTemplate redisTemplate;

    @MockBean
    private DeliveryEventProducer deliveryEventProducer;

    @AfterEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void retryUnmatched_thenAssignedAtNextCycle() {
        PendingOrder pendingOrder = PendingOrder.builder()
                .id(9001L)
                .partnerId(101L)
                .customerId(202L)
                .zoneId(1L)
                .partnerLat(36.8)
                .partnerLon(10.1)
                .customerLat(36.81)
                .customerLon(10.11)
                .guaranteedDeliveryMinutes(30)
                .createdAt(Instant.now())
                .build();

        pendingOrderRedisRepository.add(pendingOrder);

        dispatchCycleService.runCycle(1L);
        await().untilAsserted(() -> {
            long count = pendingOrderRedisRepository.countByZone(1L);
            org.junit.jupiter.api.Assertions.assertEquals(1L, count);
        });

        redisTemplate.opsForValue().set("courier:555:isOnline", "true");
        redisTemplate.opsForValue().set("courier:555:status", "IDLE");
        redisTemplate.opsForValue().set("courier:555:zone", "1");
        redisTemplate.opsForValue().set("courier:555:position", "{\"lat\":36.8001,\"lng\":10.1001}");

        dispatchCycleService.runCycle(1L);

        await().untilAsserted(() -> {
            long count = pendingOrderRedisRepository.countByZone(1L);
            org.junit.jupiter.api.Assertions.assertEquals(0L, count);
        });
        verify(deliveryEventProducer, atLeastOnce()).publishAssigned(any());
    }
}
