package com.speedline.partner.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Corps de requête pour modifier une MenuCategory.
 * Tous les champs sont optionnels (patch sémantique).
 */
@Data
public class UpdateMenuCategoryRequest {

    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;

    @Size(max = 500, message = "L'URL de l'image ne doit pas dépasser 500 caractères")
    private String imageUrl;

    private Integer position;

    private Boolean isVisible;
}
