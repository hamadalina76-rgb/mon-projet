package com.speedline.delivery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Documentation REST des contrats WebSocket de tracking.
 * Visible dans Swagger pour faciliter l'intégration front.
 */
@RestController
@RequestMapping("/api/v1/docs")
@Tag(name = "Realtime Tracking", description = "Contrats WebSocket pour le tracking livreur")
public class TrackingDocsController {

    @Operation(
            summary = "Contrats WebSocket de tracking",
            description = "Décrit les messages échangés sur ws://host/ws/location et ws://host/ws/tracking/{orderId}."
    )
    @GetMapping(value = "/tracking-websocket", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getContracts() {
        return Map.of(
                "courierToBackend", Map.of(
                        "endpoint", "ws://host/ws/location",
                        "auth", "JWT (Authorization: Bearer <token> ou paramètre token=<token>)",
                        "messageExample", Map.of(
                                "type", "POSITION_UPDATE",
                                "payload", Map.of(
                                        "lat", 36.8423,
                                        "lng", 10.1937,
                                        "accuracy", 5.0,
                                        "speed", 12.3,
                                        "heading", 180.0,
                                        "batteryLevel", 87,
                                        "timestamp", "2026-03-13T13:45:00Z"
                                )
                        )
                ),
                "backendToClient", Map.of(
                        "endpoint", "ws://host/ws/tracking/{orderId}",
                        "auth", "JWT",
                        "messageExample", Map.of(
                                "type", "COURIER_POSITION",
                                "payload", Map.of(
                                        "courierId", 123,
                                        "lat", 36.8423,
                                        "lng", 10.1937,
                                        "heading", 180.0,
                                        "estimatedArrivalMin", 5
                                )
                        )
                )
        );
    }
}

