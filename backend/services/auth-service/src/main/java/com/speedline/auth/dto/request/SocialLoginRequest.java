package com.speedline.auth.dto.request;

import com.speedline.auth.domain.AuthProvider;
import com.speedline.auth.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SocialLoginRequest {
    
    @NotBlank(message = "Access token is required")
    private String accessToken;
    
    @NotNull(message = "Provider is required")
    private AuthProvider provider;
    
    @NotNull(message = "Role is required")
    private Role role;
    
    private String deviceInfo;
}
