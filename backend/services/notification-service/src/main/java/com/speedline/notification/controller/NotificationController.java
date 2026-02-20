package com.speedline.notification.controller;

import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import com.speedline.notification.dto.SendNotificationRequest;
import com.speedline.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Notifications
 *
 * Endpoints:
 * POST   /notifications/send             - Send notification
 * GET    /notifications/{userId}          - Get user notifications
 * PUT    /notifications/{id}/read         - Mark as read
 * PUT    /notifications/{userId}/read-all - Mark all as read
 * GET    /notifications/{userId}/unread-count - Get unread count
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Send a notification
     * POST /notifications/send
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody SendNotificationRequest request) {
        log.info("Sending notification to user {}: {}", request.getUserId(), request.getTitle());

        try {
            notificationService.sendNotification(
                    request.getUserId(),
                    request.getType() != null ? request.getType() : NotificationType.SYSTEM,
                    request.getTitle(),
                    request.getMessage(),
                    request.getData(),
                    request.getChannel() != null ? request.getChannel() : NotificationChannel.IN_APP
            );
            return ResponseEntity.ok(Map.of("message", "Notification sent successfully"));
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to send notification: " + e.getMessage()));
        }
    }

    /**
     * Get user notifications
     * GET /notifications/{userId}?page=0&size=20
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting notifications for user: {}", userId);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationService.NotificationDTO> notifications =
                    notificationService.getUserNotifications(userId, pageable);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Failed to get notifications for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get notifications: " + e.getMessage()));
        }
    }

    /**
     * Mark notification as read
     * PUT /notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String id) {
        log.info("Marking notification as read: {}", id);

        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
        } catch (Exception e) {
            log.error("Failed to mark notification as read: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to mark as read: " + e.getMessage()));
        }
    }

    /**
     * Mark all notifications as read for a user
     * PUT /notifications/{userId}/read-all
     */
    @PutMapping("/{userId}/read-all")
    public ResponseEntity<?> markAllAsRead(@PathVariable Long userId) {
        log.info("Marking all notifications as read for user: {}", userId);

        try {
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (Exception e) {
            log.error("Failed to mark all as read for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to mark all as read: " + e.getMessage()));
        }
    }

    /**
     * Get unread notification count
     * GET /notifications/{userId}/unread-count
     */
    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long userId) {
        try {
            long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Failed to get unread count for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get unread count: " + e.getMessage()));
        }
    }

    /**
     * Get admin notifications (includes userId=0 broadcast + admin's userId)
     * GET /notifications/for-admin/{adminUserId}?page=0&size=20
     */
    @GetMapping("/for-admin/{adminUserId}")
    public ResponseEntity<?> getAdminNotifications(
            @PathVariable Long adminUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting admin notifications for adminUserId: {}", adminUserId);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationService.NotificationDTO> notifications =
                    notificationService.getAdminNotifications(adminUserId, pageable);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Failed to get admin notifications for {}: {}", adminUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get admin notifications: " + e.getMessage()));
        }
    }

    /**
     * Get unread count for admin
     * GET /notifications/for-admin/{adminUserId}/unread-count
     */
    @GetMapping("/for-admin/{adminUserId}/unread-count")
    public ResponseEntity<?> getUnreadCountForAdmin(@PathVariable Long adminUserId) {
        try {
            long count = notificationService.getUnreadCountForAdmin(adminUserId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Failed to get admin unread count for {}: {}", adminUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get admin unread count: " + e.getMessage()));
        }
    }

    /**
     * Mark all admin notifications as read
     * PUT /notifications/for-admin/{adminUserId}/read-all
     */
    @PutMapping("/for-admin/{adminUserId}/read-all")
    public ResponseEntity<?> markAllAsReadForAdmin(@PathVariable Long adminUserId) {
        log.info("Marking all admin notifications as read for: {}", adminUserId);
        try {
            notificationService.markAllAsReadForAdmin(adminUserId);
            return ResponseEntity.ok(Map.of("message", "All admin notifications marked as read"));
        } catch (Exception e) {
            log.error("Failed to mark all admin notifications as read for {}: {}", adminUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to mark all as read: " + e.getMessage()));
        }
    }
}
