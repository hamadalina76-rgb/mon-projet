package com.speedline.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPushTokenRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "token is required")
    private String token;

    private String deviceType; // ANDROID, IOS, WEB

    private String deviceId;
}
