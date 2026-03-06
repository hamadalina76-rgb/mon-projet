package com.speedline.partner.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Corps de requête PATCH /reorder.
 * Contient la liste des éléments avec leur nouvelle position.
 * TC-12 : PATCH /categories/reorder avec [{id:3,position:1},{id:1,position:2},{id:2,position:3}].
 */
@Data
public class ReorderRequest {

    @NotEmpty(message = "La liste de réordonnancement ne peut pas être vide")
    @Valid
    private List<ReorderItemRequest> items;
}
