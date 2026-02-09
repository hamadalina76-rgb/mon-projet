package com.speedline.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les requêtes impliquant des points de fidélité
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête contenant un nombre de points")
public class PointsRequest {

    @NotNull(message = "Le nombre de points est obligatoire")
    @Min(value = 1, message = "Le nombre de points doit être au minimum 1")
    @Schema(description = "Nombre de points de fidélité", example = "100", required = true)
    private Integer points;
}
