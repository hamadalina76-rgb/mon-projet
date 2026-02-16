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
    
    // Vehicle Info
    private String vehicleType;
    private String vehicleModel;
    private String vehicleColor;
    private String vehiclePlate;
    
    // ID Card Info
    private String identityNumber;
    
    // Driving License Info
    private String licenseNumber;
    private String licenseExpiryDate;
    
    // Bank Info
    private String accountHolderName;
    private String iban;
}
