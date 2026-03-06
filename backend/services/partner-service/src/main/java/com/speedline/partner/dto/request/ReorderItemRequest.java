package com.speedline.partner.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Représente un élément de réordonnancement : un identifiant + sa nouvelle position.
 * Utilisé dans {@link ReorderRequest}.
 * TC-12 : réordonner les catégories [3,1,2] → ordre mis à jour.
 */
@Data
public class ReorderItemRequest {

    @NotNull(message = "L'identifiant est obligatoire")
    private Long id;

    @NotNull(message = "La position est obligatoire")
    private Integer position;
}
