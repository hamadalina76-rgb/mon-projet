package com.speedline.user.dto;

import com.speedline.user.domain.CustomerPreferences;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
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
@Schema(description = "Requête de création d'un profil client")
public class CustomerCreateRequest {

    /**
     * ID de l'utilisateur dans auth-service (obligatoire)
     */
    @NotNull(message = "L'ID utilisateur est obligatoire")
    @Positive(message = "L'ID utilisateur doit être positif")
    @Schema(description = "ID de l'utilisateur dans auth-service", example = "1", required = true)
    private Long userId;

    /**
     * Code de parrainage utilisé (optionnel)
     */
    @Pattern(regexp = "^(SPD[A-Z0-9]{8})?$", message = "Format de code de parrainage invalide")
    @Schema(description = "Code de parrainage optionnel (format: SPD + 8 caractères)", example = "SPDABC12345")
    private String referralCode;

    /**
     * Préférences initiales (optionnel)
     */
    @Schema(description = "Préférences initiales du client")
    private CustomerPreferences preferences;
}
