package com.speedline.notification.service.impl;

import com.speedline.notification.domain.Notification;
import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import com.speedline.notification.domain.PushToken;
import com.speedline.notification.repository.NotificationRepository;
import com.speedline.notification.repository.PushTokenRepository;
import com.speedline.notification.service.NotificationService.NotificationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.thymeleaf.TemplateEngine;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private FCMServiceImpl fcmService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Captor
    private ArgumentCaptor<PushToken> pushTokenCaptor;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .id("notif-001")
                .userId(1L)
                .type(NotificationType.ORDER)
                .title("Order Update")
                .message("Your order has been confirmed")
                .data(Map.of("orderId", 100))
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // sendNotification
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("sendNotification")
    class SendNotification {

        @Test
        @DisplayName("should save notification and push via WebSocket for IN_APP channel")
        void shouldSaveAndPushForInAppChannel() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

            notificationService.sendNotification(
                    1L, NotificationType.ORDER, "Order Update",
                    "Your order has been confirmed",
                    Map.of("orderId", 100), NotificationChannel.IN_APP);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification saved = notificationCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getType()).isEqualTo(NotificationType.ORDER);
            assertThat(saved.getTitle()).isEqualTo("Order Update");
            assertThat(saved.getIsRead()).isFalse();
            assertThat(saved.getIsSent()).isTrue();

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/1/notifications"), any(NotificationDTO.class));
        }

        @Test
        @DisplayName("should save notification and push via WebSocket for PUSH channel")
        void shouldSaveAndPushForPushChannel() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

            notificationService.sendNotification(
                    1L, NotificationType.ORDER, "Order Update",
                    "Confirmed", null, NotificationChannel.PUSH);

            verify(notificationRepository).save(any(Notification.class));
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/1/notifications"), any(NotificationDTO.class));
            verify(fcmService).sendToUser(eq(1L), eq("Order Update"), eq("Confirmed"), anyMap());
        }

        @Test
        @DisplayName("should save notification without WebSocket push for EMAIL channel")
        void shouldSaveWithoutPushForEmailChannel() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

            notificationService.sendNotification(
                    1L, NotificationType.PAYMENT, "Payment",
                    "Paid", null, NotificationChannel.EMAIL);

            verify(notificationRepository).save(any(Notification.class));
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(NotificationDTO.class));
        }

        @Test
        @DisplayName("should not fail when FCM throws exception")
        void shouldNotFailWhenFcmThrowsException() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
            doThrow(new RuntimeException("FCM error")).when(fcmService)
                    .sendToUser(anyLong(), anyString(), anyString(), anyMap());

            notificationService.sendNotification(
                    1L, NotificationType.ORDER, "Title", "Msg",
                    Map.of("key", "val"), NotificationChannel.PUSH);

            verify(notificationRepository).save(any(Notification.class));
        }
    }

    // -------------------------------------------------------------------------
    // markAsRead
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("should mark notification as read when found")
        void shouldMarkAsReadWhenFound() {
            when(notificationRepository.findById("notif-001")).thenReturn(Optional.of(sampleNotification));
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

            notificationService.markAsRead("notif-001");

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification updated = notificationCaptor.getValue();
            assertThat(updated.getIsRead()).isTrue();
            assertThat(updated.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("should do nothing when notification not found")
        void shouldDoNothingWhenNotFound() {
            when(notificationRepository.findById("unknown")).thenReturn(Optional.empty());

            notificationService.markAsRead("unknown");

            verify(notificationRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // markAllAsRead
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("should mark all unread notifications for a user as read")
        void shouldMarkAllUnreadAsRead() {
            Notification n1 = Notification.builder().id("n1").userId(1L).isRead(false).build();
            Notification n2 = Notification.builder().id("n2").userId(1L).isRead(false).build();
            when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(n1, n2));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

            notificationService.markAllAsRead(1L);

            verify(notificationRepository, times(2)).save(any(Notification.class));
            assertThat(n1.getIsRead()).isTrue();
            assertThat(n2.getIsRead()).isTrue();
        }

        @Test
        @DisplayName("should do nothing when no unread notifications exist")
        void shouldDoNothingWhenNoUnread() {
            when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                    .thenReturn(Collections.emptyList());

            notificationService.markAllAsRead(1L);

            verify(notificationRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // getUnreadCount
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("should return the count of unread notifications")
        void shouldReturnUnreadCount() {
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

            long count = notificationService.getUnreadCount(1L);

            assertThat(count).isEqualTo(5L);
            verify(notificationRepository).countByUserIdAndIsReadFalse(1L);
        }

        @Test
        @DisplayName("should return zero when no unread notifications")
        void shouldReturnZeroWhenNoneUnread() {
            when(notificationRepository.countByUserIdAndIsReadFalse(99L)).thenReturn(0L);

            long count = notificationService.getUnreadCount(99L);

            assertThat(count).isEqualTo(0L);
        }
    }

    // -------------------------------------------------------------------------
    // getUserNotifications
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getUserNotifications")
    class GetUserNotifications {

        @Test
        @DisplayName("should return paged notifications mapped to DTO")
        void shouldReturnPagedNotifications() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = new PageImpl<>(List.of(sampleNotification), pageable, 1);
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

            Page<NotificationDTO> result = notificationService.getUserNotifications(1L, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo("notif-001");
            assertThat(result.getContent().get(0).title()).isEqualTo("Order Update");
        }
    }

    // -------------------------------------------------------------------------
    // registerPushToken
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("registerPushToken")
    class RegisterPushToken {

        @Test
        @DisplayName("should create new push token when token not found")
        void shouldCreateNewPushToken() {
            when(pushTokenRepository.findByToken("token-abc")).thenReturn(Optional.empty());
            when(pushTokenRepository.save(any(PushToken.class))).thenAnswer(inv -> inv.getArgument(0));

            notificationService.registerPushToken(1L, "token-abc", "ANDROID", "device-001");

            verify(pushTokenRepository).save(pushTokenCaptor.capture());
            PushToken saved = pushTokenCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getToken()).isEqualTo("token-abc");
            assertThat(saved.getDeviceType()).isEqualTo(PushToken.DeviceType.ANDROID);
            assertThat(saved.getDeviceId()).isEqualTo("device-001");
            assertThat(saved.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("should update existing push token when token found")
        void shouldUpdateExistingPushToken() {
            PushToken existing = PushToken.builder()
                    .id("pt-001")
                    .userId(2L)
                    .token("token-abc")
                    .deviceType(PushToken.DeviceType.IOS)
                    .isActive(false)
                    .build();
            when(pushTokenRepository.findByToken("token-abc")).thenReturn(Optional.of(existing));
            when(pushTokenRepository.save(any(PushToken.class))).thenAnswer(inv -> inv.getArgument(0));

            notificationService.registerPushToken(1L, "token-abc", "ANDROID", "device-002");

            verify(pushTokenRepository).save(pushTokenCaptor.capture());
            PushToken updated = pushTokenCaptor.getValue();
            assertThat(updated.getUserId()).isEqualTo(1L);
            assertThat(updated.getIsActive()).isTrue();
            assertThat(updated.getDeviceType()).isEqualTo(PushToken.DeviceType.ANDROID);
        }

        @Test
        @DisplayName("should not register when userId is null")
        void shouldNotRegisterWhenUserIdNull() {
            notificationService.registerPushToken(null, "token-abc", "ANDROID", "device-001");

            verify(pushTokenRepository, never()).findByToken(anyString());
            verify(pushTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not register when token is blank")
        void shouldNotRegisterWhenTokenBlank() {
            notificationService.registerPushToken(1L, "  ", "ANDROID", "device-001");

            verify(pushTokenRepository, never()).findByToken(anyString());
            verify(pushTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not register when token is null")
        void shouldNotRegisterWhenTokenNull() {
            notificationService.registerPushToken(1L, null, "ANDROID", "device-001");

            verify(pushTokenRepository, never()).findByToken(anyString());
            verify(pushTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("should default to ANDROID when device type is invalid")
        void shouldDefaultToAndroidForInvalidDeviceType() {
            when(pushTokenRepository.findByToken("token-xyz")).thenReturn(Optional.empty());
            when(pushTokenRepository.save(any(PushToken.class))).thenAnswer(inv -> inv.getArgument(0));

            notificationService.registerPushToken(1L, "token-xyz", "INVALID_TYPE", null);

            verify(pushTokenRepository).save(pushTokenCaptor.capture());
            PushToken saved = pushTokenCaptor.getValue();
            assertThat(saved.getDeviceType()).isEqualTo(PushToken.DeviceType.ANDROID);
        }
    }

    // -------------------------------------------------------------------------
    // removePushToken
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("removePushToken")
    class RemovePushToken {

        @Test
        @DisplayName("should deactivate token when found")
        void shouldDeactivateToken() {
            PushToken token = PushToken.builder().id("pt-1").token("abc").isActive(true).build();
            when(pushTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));
            when(pushTokenRepository.save(any(PushToken.class))).thenAnswer(inv -> inv.getArgument(0));

            notificationService.removePushToken("abc");

            verify(pushTokenRepository).save(pushTokenCaptor.capture());
            assertThat(pushTokenCaptor.getValue().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("should do nothing when token is null")
        void shouldDoNothingWhenNull() {
            notificationService.removePushToken(null);
            verify(pushTokenRepository, never()).findByToken(anyString());
        }

        @Test
        @DisplayName("should do nothing when token is blank")
        void shouldDoNothingWhenBlank() {
            notificationService.removePushToken("  ");
            verify(pushTokenRepository, never()).findByToken(anyString());
        }
    }

    // -------------------------------------------------------------------------
    // sendAdminBroadcast
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("sendAdminBroadcast")
    class SendAdminBroadcast {

        @Test
        @DisplayName("should save broadcast notification and push to admin topic")
        void shouldSaveBroadcastAndPushToAdminTopic() {
            Notification broadcast = Notification.builder()
                    .id("bc-001").userId(0L).type(NotificationType.SYSTEM)
                    .title("System Alert").message("Maintenance").isRead(false)
                    .channel(NotificationChannel.IN_APP).createdAt(LocalDateTime.now())
                    .build();
            when(notificationRepository.save(any(Notification.class))).thenReturn(broadcast);

            notificationService.sendAdminBroadcast(
                    NotificationType.SYSTEM, "System Alert", "Maintenance", null);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification saved = notificationCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo(0L);
            assertThat(saved.getChannel()).isEqualTo(NotificationChannel.IN_APP);
            verify(messagingTemplate).convertAndSend(eq("/topic/admin/notifications"), any(NotificationDTO.class));
        }
    }

    // -------------------------------------------------------------------------
    // Admin notifications
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAdminNotifications")
    class AdminNotifications {

        @Test
        @DisplayName("should query notifications for userId=0 and adminUserId")
        void shouldQueryAdminNotifications() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = new PageImpl<>(List.of(sampleNotification), pageable, 1);
            when(notificationRepository.findByUserIdInOrderByCreatedAtDesc(
                    List.of(0L, 5L), pageable)).thenReturn(page);

            Page<NotificationDTO> result = notificationService.getAdminNotifications(5L, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return unread count for admin")
        void shouldReturnUnreadCountForAdmin() {
            when(notificationRepository.countByUserIdInAndIsReadFalse(List.of(0L, 5L))).thenReturn(3L);

            long count = notificationService.getUnreadCountForAdmin(5L);

            assertThat(count).isEqualTo(3L);
        }
    }
}
