package com.speedline.auth.controller;

import com.speedline.auth.domain.User;
import com.speedline.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal REST Controller for inter-service communication
 * NOT exposed via API Gateway - only for internal microservice calls
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserRepository userRepository;

    /**
     * Update partner ID for a user (called by partner-service)
     * PUT /internal/users/{userId}/partner-id
     */
    @PutMapping("/{userId}/partner-id")
    public ResponseEntity<?> updatePartnerId(
            @PathVariable Long userId,
            @RequestBody Map<String, Long> body) {
        
        log.info("========== RECEIVED UPDATE PARTNER ID REQUEST ==========");
        log.info("UserId: {}, Body: {}", userId, body);
        
        Long partnerId = body.get("partnerId");
        log.info("Extracted partnerId: {}", partnerId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            log.info("Found user: id={}, email={}, current partnerId={}", 
                     user.getId(), user.getEmail(), user.getPartnerId());
            
            user.setPartnerId(partnerId);
            User savedUser = userRepository.save(user);

            log.info("✅ Successfully updated partnerId for user {} to {}", userId, savedUser.getPartnerId());
            log.info("========== UPDATE COMPLETED ==========");
            
            return ResponseEntity.ok(Map.of(
                    "message", "Partner ID updated successfully",
                    "userId", userId,
                    "partnerId", partnerId
            ));
        } catch (Exception e) {
            log.error("========== ERROR UPDATING PARTNER ID ==========");
            log.error("UserId: {}, PartnerId: {}, Error: {}", userId, partnerId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update partner ID: " + e.getMessage()));
        }
    }
}
