package com.speedline.user.controller;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.dto.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final AuthServiceClient authServiceClient;

    @GetMapping("/test-auth-service/{userId}")
    public ResponseEntity<Map<String, Object>> testAuthService(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Testing connection to auth-service for userId: {}", userId);
            UserInfoDTO userInfo = authServiceClient.getUserById(userId);
            response.put("status", "success");
            response.put("userInfo", userInfo);
            log.info("✅ Successfully retrieved user info: {} {}", userInfo.getFirstName(), userInfo.getLastName());
        } catch (Exception e) {
            log.error("❌ Failed to reach auth-service", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("errorType", e.getClass().getName());
        }
        return ResponseEntity.ok(response);
    }
}
