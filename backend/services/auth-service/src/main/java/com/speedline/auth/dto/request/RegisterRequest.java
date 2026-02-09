package com.speedline.auth.dto.request;

import com.speedline.auth.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "saif@gmail.com")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(\\+216)?[234579]\\d{7}$",
            message = "Invalid Tunisian phone number"
    )
    @Schema(
            example = "+21623456789"
    )
    private String phoneNumber;
    
    @NotNull(message = "Role is required")
    private Role role;
}
