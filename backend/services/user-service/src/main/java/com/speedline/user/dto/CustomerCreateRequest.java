package com.speedline.user.dto;

import com.speedline.user.domain.CustomerPreferences;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer un nouveau profil client
 * Appelé après l'inscription dans auth-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreateRequest {

    /**
     * ID de l'utilisateur dans auth-service (obligatoire)
     */
    @NotNull(message = "L'ID utilisateur est obligatoire")
    private Long userId;

    /**
     * Code de parrainage utilisé (optionnel)
     */
    private String referralCode;

    /**
     * Préférences initiales (optionnel)
     */
    private CustomerPreferences preferences;
}
