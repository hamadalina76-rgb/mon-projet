package com.speedline.partner.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Corps de requête PATCH /products/promotions.
 * Définit ou supprime le label et la date de fin de promotion pour une liste de produits.
 * La réduction et le prix promo sont calculés et persistés côté backend (price, originalPrice, discountPercentage).
 */
@Data
public class SetPromotionRequest {

    @NotNull(message = "productIds is required")
    private List<Long> productIds;

    /** Label affiché en badge (ex. "-20%", "Nouveau"). Null ou vide = supprimer la promo. */
    private String promotionLabel;

    /** Date de début de la promotion. Null = pas de début. */
    private LocalDate promotionStartDate;

    /** Date de fin de la promotion. Null = pas de fin. */
    private LocalDate promotionEndDate;

    /** Pourcentage de réduction (0-100). Si renseigné, le backend calcule et enregistre price, originalPrice, discountPercentage. */
    @Min(value = 0, message = "discountPercentage must be between 0 and 100")
    @Max(value = 100, message = "discountPercentage must be between 0 and 100")
    private Integer discountPercentage;
}
