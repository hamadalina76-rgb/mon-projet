package com.speedline.partner.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse complète d'un produit avec ses groupes d'options.
 * TC-10 : produit lié à la catégorie, price retourné.
 * TC-13 : isAvailable=false → produit non commandable.
 * TC-16 : inclus dans la structure imbriquée du menu complet.
 */
@Data
@Builder
public class ProductResponse {

    private Long id;
    private Long categoryId;
    private Long partnerId;

    private String name;
    private String description;

    /** URL de l'image principale. */
    private String imageUrl;

    /** Prix de base du produit. */
    private BigDecimal price;

    /** TC-13 : false → produit non commandable côté client. */
    private Boolean isAvailable;

    private Boolean isPopular;

    /** Temps de préparation en minutes. */
    private Integer preparationTimeMin;

    /** Position dans la catégorie (ordre croissant). */
    private Integer position;

    /** Tags libres séparés par virgules. */
    private String tags;

    /**
     * Groupes d'options du produit (taille, cuisson, sauces…).
     * TC-16 : inclus dans le menu complet.
     */
    private List<OptionGroupResponse> optionGroups;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
