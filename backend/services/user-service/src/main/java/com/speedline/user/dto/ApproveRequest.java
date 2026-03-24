package com.speedline.user.dto;

import com.speedline.user.domain.CourierType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Corps de la requête d'approbation d'un livreur.
 * L'admin doit choisir le type (INTERNAL ou EXTERNAL) avant de valider.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête d'approbation d'un livreur")
public class ApproveRequest {

    @NotNull(message = "Le type de livreur est obligatoire (INTERNAL ou EXTERNAL)")
    @Schema(description = "Type de livreur", example = "INTERNAL", allowableValues = {"INTERNAL", "EXTERNAL"})
    private CourierType courierType;
}
