package com.speedline.delivery.dispatch.integration;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.scheduler.PartnerDelayScheduler;
import com.speedline.delivery.dispatch.service.CourierResponseTimeoutTracker;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "eureka.client.enabled=false",
        "dispatch.interval-seconds=60",
        "dispatch.partner-delay.threshold-minutes=5"
})
@EnabledIfSystemProperty(named = "runDockerTests", matches = "true")
@SuppressWarnings("resource")
class PartnerDelayFlowIntegrationTest {

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
    @Qualifier("partnerDelayMessageHandler")
    private MessageHandler partnerDelayMessageHandler;
    @Autowired
    private PartnerDelayScheduler partnerDelayScheduler;
    @Autowired
    private CourierResponseTimeoutTracker timeoutTracker;
    @Autowired
    private PendingOrderRedisRepository pendingOrderRedisRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void delayOverThreshold_releasesCourier_requeuesAndIncrementsSla() {
        Long orderId = 9901L;
        Long partnerId = 420L;
        Long courierId = 840L;
        Long zoneId = 6L;

        timeoutTracker.trackProposal(
                orderId,
                courierId,
                java.time.Duration.ofSeconds(45),
                PendingOrder.builder()
                        .id(orderId)
                        .partnerId(partnerId)
                        .customerId(18L)
                        .zoneId(zoneId)
                        .partnerLat(36.8)
                        .partnerLon(10.1)
                        .customerLat(36.82)
                        .customerLon(10.12)
                        .createdAt(Instant.now())
                        .build()
        );
        redisTemplate.opsForValue().set("courier:" + courierId + ":status", "ON_DELIVERY");

        String payload = "{" +
                "\"eventType\":\"PARTNER_NOT_READY\"," +
                "\"orderId\":" + orderId + "," +
                "\"partnerId\":" + partnerId + "," +
                "\"courierId\":" + courierId + "," +
                "\"delayStartedAt\":\"" + Instant.now().minusSeconds(6 * 60L) + "\"" +
                "}";
        partnerDelayMessageHandler.handleMessage(MessageBuilder.withPayload(payload).build());
        partnerDelayScheduler.checkActivePartnerDelays();

        assertEquals("IDLE", redisTemplate.opsForValue().get("courier:" + courierId + ":status"));
        assertEquals(1L, pendingOrderRedisRepository.countByZone(zoneId));
        String slaKey = "partner:%d:sla:delays:%s".formatted(partnerId, YearMonth.now(ZoneOffset.UTC));
        assertEquals("1", redisTemplate.opsForValue().get(slaKey));
    }
}
