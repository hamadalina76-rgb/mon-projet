package com.speedline.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entité PushToken - Tokens FCM pour notifications push
 */
@Document(collection = "push_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushToken {

    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed(unique = true)
    private String token;

    private DeviceType deviceType;

    private String deviceId;

    private String deviceModel;

    private String osVersion;

    private String appVersion;

    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;

    public enum DeviceType {
        ANDROID,
        IOS,
        WEB
    }
}
