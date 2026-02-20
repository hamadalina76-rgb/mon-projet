package com.speedline.notification.service.impl;

import com.speedline.notification.domain.Notification;
import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import com.speedline.notification.repository.NotificationRepository;
import com.speedline.notification.service.NotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Implementation of NotificationService
 * Handles notification persistence in MongoDB and real-time WebSocket push
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendNotification(Long userId, NotificationType type, String title,
                                String message, Map<String, Object> data, NotificationChannel channel) {
        log.info("Sending {} notification to user {}: {}", type, userId, title);

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .channel(channel)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification saved with id: {}", notification.getId());

        // Push via WebSocket if IN_APP channel
        if (channel == NotificationChannel.IN_APP || channel == NotificationChannel.PUSH) {
            pushToWebSocket(userId, notification);
        }
    }

    @Override
    public void sendPushNotification(Long userId, String title, String message, Map<String, Object> data) {
        sendNotification(userId, NotificationType.SYSTEM, title, message, data, NotificationChannel.PUSH);
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("SMS sending not yet implemented. Phone: {}, Message: {}", phoneNumber, message);
    }

    @Override
    public void sendEmail(String email, String subject, String templateName, Map<String, Object> variables) {
        log.info("Sending email to: {} with template: {}", email, templateName);
        
        try {
            // Create Thymeleaf context with variables
            Context context = new Context();
            context.setVariables(variables);
            
            // Ensure template name has .html extension if not present
            String templatePath = templateName.endsWith(".html") ? templateName : templateName + ".html";
            
            // Process template
            String htmlContent = templateEngine.process(templatePath, context);
            
            // Create and send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@speedline.com");
            
            mailSender.send(message);
            
            log.info("Email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(Long userId, Pageable pageable) {
        log.info("Getting notifications for user: {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    @Override
    public void markAsRead(String notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    @Override
    public void markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .forEach(notification -> {
                    notification.setIsRead(true);
                    notification.setReadAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getAdminNotifications(Long adminUserId, Pageable pageable) {
        log.info("Getting admin notifications for adminUserId: {}", adminUserId);
        List<Long> userIds = Arrays.asList(0L, adminUserId);
        return notificationRepository.findByUserIdInOrderByCreatedAtDesc(userIds, pageable)
                .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCountForAdmin(Long adminUserId) {
        log.info("Getting unread count for admin: {}", adminUserId);
        List<Long> userIds = Arrays.asList(0L, adminUserId);
        return notificationRepository.countByUserIdInAndIsReadFalse(userIds);
    }

    @Override
    public void markAllAsReadForAdmin(Long adminUserId) {
        log.info("Marking all as read for admin: {}", adminUserId);
        List<Long> userIds = Arrays.asList(0L, adminUserId);
        notificationRepository.findByUserIdInAndIsReadFalse(userIds)
                .forEach(notification -> {
                    notification.setIsRead(true);
                    notification.setReadAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                });
    }

    @Override
    public void registerPushToken(Long userId, String token, String deviceType, String deviceId) {
        log.info("Push token registration not yet implemented. UserId: {}, Token: {}", userId, token);
    }

    @Override
    public void removePushToken(String token) {
        log.info("Push token removal not yet implemented. Token: {}", token);
    }

    /**
     * Push notification to WebSocket subscribers
     */
    private void pushToWebSocket(Long userId, Notification notification) {
        try {
            NotificationDTO dto = toDTO(notification);
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", dto);
            log.info("WebSocket push sent to user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Push notification to admin topic
     */
    public void pushToAdminTopic(Notification notification) {
        try {
            NotificationDTO dto = toDTO(notification);
            messagingTemplate.convertAndSend("/topic/admin/notifications", dto);
            log.info("WebSocket push sent to admin topic");
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to admin topic: {}", e.getMessage());
        }
    }

    /**
     * Push notification to specific partner topic
     */
    public void pushToPartnerTopic(Long partnerId, Notification notification) {
        try {
            NotificationDTO dto = toDTO(notification);
            messagingTemplate.convertAndSend("/topic/partner/" + partnerId + "/notifications", dto);
            log.info("WebSocket push sent to partner {}", partnerId);
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to partner {}: {}", partnerId, e.getMessage());
        }
    }

    private NotificationDTO toDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getData(),
                notification.getIsRead(),
                notification.getChannel(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
