package com.speedline.partner.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Corps de requête pour créer un produit.
 * TC-10 : POST /api/partners/{id}/menu/products avec price=800 → HTTP 201.
 */
@Data
public class CreateProductRequest {

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères")
    private String name;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.00", message = "Le prix ne peut pas être négatif")
    private BigDecimal price;

    /** Catégorie de menu à laquelle ce produit appartient (optionnel). */
    private Long categoryId;

    private String description;

    /** URL de l'image principale. */
    @Size(max = 500)
    private String imageUrl;

    /** Disponible à la vente (défaut = true). TC-13. */
    private Boolean isAvailable;

    /** Mis en avant comme produit populaire (défaut = false). */
    private Boolean isPopular;

    /** Temps de préparation en minutes (null = temps par défaut du partenaire). */
    private Integer preparationTimeMin;

    /**
     * Position dans la catégorie (null = auto : max + 1).
     * TC-18 pattern pour les positions automatiques.
     */
    private Integer position;

    /**
     * Tags libres séparés par virgules (ex: "signature,halal").
     */
    @Size(max = 500)
    private String tags;
}
