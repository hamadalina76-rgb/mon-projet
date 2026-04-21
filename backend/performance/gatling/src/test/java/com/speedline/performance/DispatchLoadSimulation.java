package com.speedline.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * DISP-106 load profile:
 * - 500 order creations over 1 minute
 * - completion tracking by polling dispatch pending endpoint
 */
public class DispatchLoadSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String AUTH_TOKEN = System.getProperty("authToken", "");
    private static final int USER_ID = Integer.parseInt(System.getProperty("userId", "10001"));
    private static final int TOTAL_ORDERS = Integer.parseInt(System.getProperty("orders", "500"));
    private static final int WINDOW_SECONDS = Integer.parseInt(System.getProperty("windowSeconds", "60"));
    private static final int TRACKING_TIMEOUT_SECONDS = Integer.parseInt(System.getProperty("trackingTimeoutSeconds", "300"));

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .header("X-User-Id", String.valueOf(USER_ID))
            .header("X-User-Name", "gatling-user")
            .header("Authorization", AUTH_TOKEN.isBlank() ? "" : "Bearer " + AUTH_TOKEN);

    private final FeederBuilder.FileBased<Object> orderFeeder = csv("orders-feed.csv").circular();

    private final ChainBuilder createOrder = feed(orderFeeder)
            .exec(http("create_order")
                    .post("/api/v1/orders")
                    .body(StringBody(session -> "{\n" +
                            "  \"paymentMethod\": \"CASH\",\n" +
                            "  \"cartItems\": [\n" +
                            "    {\n" +
                            "      \"productId\": \"" + session.getString("productId") + "\",\n" +
                            "      \"partnerId\": \"" + session.getString("partnerId") + "\",\n" +
                            "      \"productName\": \"Burger\",\n" +
                            "      \"quantity\": 1,\n" +
                            "      \"unitPrice\": 12.0\n" +
                            "    }\n" +
                            "  ],\n" +
                            "  \"deliveryAddressDetails\": {\n" +
                            "    \"deliveryAddress\": \"Rue de test\",\n" +
                            "    \"city\": \"Tunis\",\n" +
                            "    \"deliveryLatitude\": " + session.getString("lat") + ",\n" +
                            "    \"deliveryLongitude\": " + session.getString("lon") + "\n" +
                            "  }\n" +
                            "}"))
                    .check(status().in(200, 201))
                    .check(jsonPath("$.id").optional().saveAs("orderId")));

    private final ChainBuilder trackDispatchCompletion = asLongAsDuring(
            session -> session.contains("orderId"),
            Duration.ofSeconds(TRACKING_TIMEOUT_SECONDS)
    ).on(
            pause(1)
                    .exec(http("poll_pending_orders")
                            .get("/api/v1/dispatch/orders/pending")
                            .check(status().is(200))
                            .check(bodyString().saveAs("pendingBody")))
                    .doIf(session -> {
                        String orderId = session.getString("orderId");
                        String pending = session.getString("pendingBody");
                        return pending != null && !pending.contains("\"id\":" + orderId);
                    }).then(
                            exec(session -> session.remove("orderId"))
                    )
    );

    private final ScenarioBuilder dispatchLoadScenario = scenario("dispatch_500_orders_one_minute")
            .exec(createOrder)
            .exec(trackDispatchCompletion);

    {
        setUp(
                dispatchLoadScenario.injectOpen(
                        rampUsers(TOTAL_ORDERS).during(WINDOW_SECONDS)
                )
        )
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().percentile3().lt(300000),
                        global().successfulRequests().percent().gte(95.0)
                );
    }
}

