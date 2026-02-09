package com.speedline.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les requêtes de code de parrainage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête contenant un code de parrainage")
public class ReferralCodeRequest {

    @NotBlank(message = "Le code de parrainage est obligatoire")
    @Pattern(regexp = "^SPD[A-Z0-9]{8}$", message = "Format de code de parrainage invalide")
    @Schema(description = "Code de parrainage (format: SPD + 8 caractères)", example = "SPDABC12345", required = true)
    private String referralCode;
}
