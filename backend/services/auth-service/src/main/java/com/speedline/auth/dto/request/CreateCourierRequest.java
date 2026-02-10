package com.speedline.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer un profil livreur via Feign Client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourierRequest {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    // vehicleType and other details can be added later by the courier
}
