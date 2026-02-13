package com.speedline.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO when authentication requires OTP verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {

    private String message;
    private String email;
    private Boolean otpSent;
    private Integer expirationMinutes;
}
