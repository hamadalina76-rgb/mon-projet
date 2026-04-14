package com.speedline.notification.controller;

import com.speedline.notification.dto.RegisterPushTokenRequest;
import com.speedline.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Compatibility controller for push token routes.
 *
 * Supports legacy gateway path /push-tokens and modern /api/push-tokens.
 */
@RestController
@RequestMapping("/push-tokens")
@RequiredArgsConstructor
@Slf4j
public class PushTokenController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<?> registerPushToken(@Valid @RequestBody RegisterPushTokenRequest request) {
        log.info("Registering push token on /push-tokens for user: {}", request.getUserId());
        try {
            notificationService.registerPushToken(
                    request.getUserId(),
                    request.getToken(),
                    request.getDeviceType(),
                    request.getDeviceId()
            );
            return ResponseEntity.ok(Map.of("message", "Push token registered"));
        } catch (Exception e) {
            log.error("Failed to register push token on /push-tokens: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to register push token: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<?> removePushToken(@PathVariable String token) {
        final String normalized = token == null ? "" : token.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }

        notificationService.removePushToken(normalized);
        return ResponseEntity.ok(Map.of("message", "Push token removed"));
    }
}
