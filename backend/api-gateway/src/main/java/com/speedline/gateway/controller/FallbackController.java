package com.speedline.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/fallback/order")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        return buildFallbackResponse("order-service");
    }

    @GetMapping("/fallback/payment")
    public ResponseEntity<Map<String, Object>> paymentFallback() {
        return buildFallbackResponse("payment-service");
    }

    @GetMapping("/fallback/delivery")
    public ResponseEntity<Map<String, Object>> deliveryFallback() {
        return buildFallbackResponse("delivery-service");
    }

    @GetMapping("/fallback/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        return buildFallbackResponse("user-service");
    }

    @GetMapping("/fallback/partner")
    public ResponseEntity<Map<String, Object>> partnerFallback() {
        return buildFallbackResponse("partner-service");
    }

    @GetMapping("/fallback/notification")
    public ResponseEntity<Map<String, Object>> notificationFallback() {
        return buildFallbackResponse("notification-service");
    }

    @GetMapping("/fallback/location")
    public ResponseEntity<Map<String, Object>> locationFallback() {
        return buildFallbackResponse("location-service");
    }

    @GetMapping("/fallback/analytics")
    public ResponseEntity<Map<String, Object>> analyticsFallback() {
        return buildFallbackResponse("analytics-service");
    }

    @GetMapping("/fallback/promotion")
    public ResponseEntity<Map<String, Object>> promotionFallback() {
        return buildFallbackResponse("promotion-service");
    }

    @GetMapping("/fallback/review")
    public ResponseEntity<Map<String, Object>> reviewFallback() {
        return buildFallbackResponse("review-service");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String serviceName) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", serviceName,
                        "status", "UNAVAILABLE",
                        "message", "Le service est temporairement indisponible. Merci de réessayer plus tard."
                ));
    }
}

