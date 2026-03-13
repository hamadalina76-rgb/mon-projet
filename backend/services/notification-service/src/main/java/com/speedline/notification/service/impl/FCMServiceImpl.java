package com.speedline.notification.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.speedline.notification.repository.PushTokenRepository;
import com.speedline.notification.domain.PushToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sends FCM push notifications to device tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FCMServiceImpl {

    private final PushTokenRepository pushTokenRepository;

    /**
     * Send push notification to all active device tokens for the user.
     * No-op if Firebase is not initialized or user has no tokens.
     */
    public void sendToUser(Long userId, String title, String body, Map<String, Object> data) {
        if (FirebaseApp.getApps() == null || FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase not initialized, skipping FCM for user {}", userId);
            return;
        }
        List<PushToken> tokens = pushTokenRepository.findByUserIdAndIsActiveTrue(userId);
        if (tokens.isEmpty()) {
            log.debug("No push tokens for user {}, skipping FCM", userId);
            return;
        }
        Map<String, String> dataMap = data != null
                ? data.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())))
                : new HashMap<>();
        for (PushToken pt : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(pt.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title != null ? title : "SpeedLine")
                                .setBody(body != null ? body : "")
                                .build())
                        .putAllData(dataMap)
                        .build();
                FirebaseMessaging.getInstance().send(message);
                log.info("FCM sent to user {} device {}", userId, pt.getDeviceType());
            } catch (FirebaseMessagingException e) {
                log.warn("FCM send failed for user {} token {}: {}", userId, pt.getId(), e.getMessage());
                if (e.getMessagingErrorCode() != null && (
                        e.getMessagingErrorCode().name().contains("INVALID_ARGUMENT") ||
                        e.getMessagingErrorCode().name().contains("UNREGISTERED"))) {
                    pt.setIsActive(false);
                    pushTokenRepository.save(pt);
                }
            }
        }
    }
}
