package com.speedline.delivery.dispatch.e2e.support;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "eureka.client.enabled=false",
        "dispatch.interval-seconds=1",
        "dispatch.eligibility.order-waiting-threshold-seconds=3",
        "dispatch.refusal.internal-warning-threshold=1",
        "dispatch.refusal.internal-hr-threshold=2",
        "dispatch.refusal.external-score-degradation-threshold=2",
        "dispatch.response-timeout.deadline-seconds=5",
        "dispatch.solver.greedy-max-orders=1",
        "dispatch.solver.hungarian-max-orders=2"
})
@Testcontainers
@EnabledIfSystemProperty(named = "runDockerTests", matches = "true")
public abstract class DispatchE2EBaseTest {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("speedline_delivery")
            .withUsername("postgres")
            .withPassword("postgres");

    protected static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    protected static final PubSubEmulatorContainer PUBSUB =
            new PubSubEmulatorContainer(
                    DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:441.0.0-emulators"));

    /**
     * Lightweight fake solver endpoint, enough for deterministic E2E.
     */
    protected static final GenericContainer<?> SOLVER = new GenericContainer<>("python:3.11-alpine")
            .withExposedPorts(5001)
            .withCommand("sh", "-c",
                    "pip install --no-cache-dir fastapi uvicorn && " +
                            "cat > /tmp/app.py <<'PY'\n" +
                            "from fastapi import FastAPI\n" +
                            "app = FastAPI()\n" +
                            "@app.get('/health')\n" +
                            "def health():\n" +
                            "    return {'status':'ok'}\n" +
                            "@app.post('/solve')\n" +
                            "def solve(req: dict):\n" +
                            "    orders=req.get('orders',[])\n" +
                            "    couriers=req.get('couriers',[])\n" +
                            "    out=[]\n" +
                            "    for i,o in enumerate(orders):\n" +
                            "        if not couriers:\n" +
                            "            break\n" +
                            "        c=couriers[i % len(couriers)]\n" +
                            "        out.append({'orderId':o.get('id'),'courierId':c.get('id'),'bundleId':('BUNDLE-E2E' if len(orders)>=3 else None),'cost':1.0,'etaPickupMin':3,'etaDeliveryMin':10})\n" +
                            "    return {'assignments': out}\n" +
                            "PY\n" +
                            "python /tmp/app.py");

    @BeforeAll
    static void bootContainers() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping DISP-106 tests.");
        POSTGRES.start();
        REDIS.start();
        PUBSUB.start();
        SOLVER.start();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.cloud.gcp.pubsub.emulator-host", PUBSUB::getEmulatorEndpoint);
        registry.add("dispatch.solver.ortools.url",
                () -> "http://" + SOLVER.getHost() + ":" + SOLVER.getMappedPort(5001));
    }

    @Autowired
    protected PendingOrderRedisRepository pendingOrders;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @SpyBean
    protected DeliveryEventProducer deliveryEventProducer;

    @Autowired
    @Qualifier("orderEventsMessageHandler")
    protected MessageHandler orderEventsMessageHandler;

    @Autowired
    @Qualifier("courierResponseMessageHandler")
    protected MessageHandler courierResponseMessageHandler;

    @AfterEach
    void resetRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    protected void pushOrderCreated(long orderId, long partnerId, long customerId, long zoneId, boolean largeOrder, Instant createdAt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "ORDER_CREATED");
        payload.put("orderId", orderId);
        payload.put("partnerId", partnerId);
        payload.put("customerId", customerId);
        payload.put("zoneId", zoneId);
        payload.put("partnerLat", 36.80);
        payload.put("partnerLon", 10.10);
        payload.put("customerLat", 36.81);
        payload.put("customerLon", 10.11);
        payload.put("guaranteedDeliveryMinutes", 30);
        payload.put("isLargeOrder", largeOrder);
        payload.put("createdAt", createdAt.toString());
        orderEventsMessageHandler.handleMessage(MessageBuilder.withPayload(asJson(payload)).build());
    }

    protected void pushCourierRefusal(long orderId, long courierId, String reason) {
        Map<String, Object> payload = Map.of(
                "eventType", "COURIER_ORDER_REFUSED",
                "orderId", orderId,
                "courierId", courierId,
                "reason", reason
        );
        courierResponseMessageHandler.handleMessage(MessageBuilder.withPayload(asJson(payload)).build());
    }

    protected void seedCourier(long courierId, long zoneId, String type, String status) {
        redisTemplate.opsForValue().set("courier:" + courierId + ":isOnline", "true");
        redisTemplate.opsForValue().set("courier:" + courierId + ":status", status);
        redisTemplate.opsForValue().set("courier:" + courierId + ":type", type);
        redisTemplate.opsForValue().set("courier:" + courierId + ":zone", String.valueOf(zoneId));
        redisTemplate.opsForValue().set("courier:" + courierId + ":position", "{\"lat\":36.8001,\"lng\":10.1001}");
    }

    private static String asJson(Map<String, Object> payload) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            if (!first) {
                b.append(',');
            }
            first = false;
            b.append('"').append(e.getKey()).append('"').append(':');
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) {
                b.append(v);
            } else {
                b.append('"').append(String.valueOf(v).replace("\"", "\\\"")).append('"');
            }
        }
        b.append('}');
        return b.toString();
    }
}

