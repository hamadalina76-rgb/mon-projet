package com.speedline.delivery.dispatch.integration;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import com.speedline.delivery.dispatch.service.DispatchCycleService;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "eureka.client.enabled=false",
        "dispatch.interval-seconds=60"
})
@EnabledIfSystemProperty(named = "runDockerTests", matches = "true")
class DispatchEligibilityIntegrationTest {

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
    void scenarioTwoInternalsThreeExternals_assignsOnlyInternalCouriers() {
        pendingOrderRedisRepository.add(buildOrder(9101L, 1L));
        pendingOrderRedisRepository.add(buildOrder(9102L, 1L));

        upsertCourier(1001L, 1L, "INTERNAL", "IDLE");
        upsertCourier(1002L, 1L, "INTERNAL", "IDLE");
        upsertCourier(2001L, 1L, "EXTERNAL", "IDLE");
        upsertCourier(2002L, 1L, "EXTERNAL", "IDLE");
        upsertCourier(2003L, 1L, "EXTERNAL", "IDLE");

        dispatchCycleService.runCycle(1L);

        await().untilAsserted(() -> assertEquals(0L, pendingOrderRedisRepository.countByZone(1L)));

        ArgumentCaptor<DispatchAssignedEvent> captor = ArgumentCaptor.forClass(DispatchAssignedEvent.class);
        verify(deliveryEventProducer, atLeast(2)).publishAssigned(captor.capture());
        List<DispatchAssignedEvent> assignedEvents = captor.getAllValues();

        Set<Long> internalIds = Set.of(1001L, 1002L);
        long internalAssignments = assignedEvents.stream()
                .map(DispatchAssignedEvent::getCourierId)
                .filter(internalIds::contains)
                .count();

        assertTrue(internalAssignments >= 2, "Expected at least two assignments to internal couriers");
        assertTrue(assignedEvents.stream().allMatch(e -> internalIds.contains(e.getCourierId())),
                "Expected only internal couriers in assignments");
    }

    private PendingOrder buildOrder(Long orderId, Long zoneId) {
        return PendingOrder.builder()
                .id(orderId)
                .partnerId(11L)
                .customerId(22L)
                .zoneId(zoneId)
                .partnerLat(36.8)
                .partnerLon(10.1)
                .customerLat(36.81)
                .customerLon(10.12)
                .guaranteedDeliveryMinutes(35)
                .createdAt(Instant.now())
                .build();
    }

    private void upsertCourier(Long courierId, Long zoneId, String type, String status) {
        String prefix = "courier:" + courierId;
        redisTemplate.opsForValue().set(prefix + ":isOnline", "true");
        redisTemplate.opsForValue().set(prefix + ":zone", String.valueOf(zoneId));
        redisTemplate.opsForValue().set(prefix + ":type", type);
        redisTemplate.opsForValue().set(prefix + ":status", status);
        redisTemplate.opsForValue().set(prefix + ":position", "{\"lat\":36.82,\"lng\":10.21}");
    }
}
