package com.speedline.delivery.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoints REST pour interroger la position actuelle d'un livreur.
 * Lit les données stockées par le WebSocket handler dans Redis.
 */
@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Courier Tracking", description = "Endpoints REST pour consulter la position et le statut d'un livreur")
public class TrackingController {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Retourne la dernière position connue d'un livreur à partir de Redis.
     *
     * Exemple de réponse:
     * {
     *   "courierId": "88",
     *   "position": {
     *     "lat": 36.8423,
     *     "lng": 10.1937,
     *     "accuracy": 5.0,
     *     "speed": 12.3,
     *     "heading": 180.0,
     *     "batteryLevel": 87,
     *     "timestamp": "2026-03-16T10:15:00Z"
     *   },
     *   "isOnline": true,
     *   "ttlSeconds": 25,
     *   "retrievedAt": "2026-03-16T10:15:30Z"
     * }
     */
    @Operation(summary = "Obtenir la dernière position connue d'un livreur")
    @GetMapping("/couriers/{courierId}")
    public ResponseEntity<?> getCourierPosition(@PathVariable String courierId) {
        try {
            String positionKey = "courier:%s:position".formatted(courierId);
            String onlineKey = "courier:%s:isOnline".formatted(courierId);

            String rawPosition = stringRedisTemplate.opsForValue().get(positionKey);
            if (rawPosition == null || rawPosition.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> position = objectMapper.readValue(
                    rawPosition,
                    new TypeReference<Map<String, Object>>() {}
            );

            String onlineValue = stringRedisTemplate.opsForValue().get(onlineKey);
            boolean isOnline = "true".equalsIgnoreCase(onlineValue);

            Long ttl = stringRedisTemplate.getExpire(positionKey);

            Map<String, Object> body = Map.of(
                    "courierId", courierId,
                    "position", position,
                    "isOnline", isOnline,
                    "ttlSeconds", ttl,
                    "retrievedAt", Instant.now().toString()
            );

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Failed to read position for courier {} from Redis: {}", courierId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to read position", "details", e.getMessage())
            );
        }
    }
}

