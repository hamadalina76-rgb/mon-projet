package com.speedline.partner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Corps de requête pour créer une MenuCategory.
 * TC-09 : POST /api/partners/{id}/menu/categories → HTTP 201, id retourné, position auto.
 */
@Data
public class CreateMenuCategoryRequest {

    /** Nom de la catégorie — obligatoire. */
    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    /** Description (optionnelle). */
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;

    /** URL de l'image d'illustration (optionnelle). */
    @Size(max = 500, message = "L'URL de l'image ne doit pas dépasser 500 caractères")
    private String imageUrl;

    /**
     * Position dans le menu (optionnelle).
     * Si null, calculée automatiquement : max(position) + 1.
     */
    private Integer position;

    /**
     * Visible dans le menu client (optionnel, défaut = true).
     */
    private Boolean isVisible;
}
