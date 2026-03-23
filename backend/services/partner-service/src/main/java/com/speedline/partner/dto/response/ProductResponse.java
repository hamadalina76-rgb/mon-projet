package com.speedline.partner.dto.response;

import com.speedline.partner.domain.ProductModerationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /** Prix de base ou prix promo (après réduction). */
    private BigDecimal price;

    /** Prix avant réduction (affiché barré si discountPercentage > 0). */
    private BigDecimal originalPrice;

    /** Pourcentage de réduction (ex. 20 pour -20%). */
    private BigDecimal discountPercentage;

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

    /** Optionnel : IN_STOCK, LOW_STOCK, OUT_OF_STOCK (enrichi depuis ProductStock). */
    private String stockStatus;

    /** Label promo affiché en badge (ex. "-20%", "Nouveau"). */
    private String promotionLabel;

    /** Date de début de la promotion. */
    private LocalDate promotionStartDate;

    /** Date de fin de la promotion. */
    private LocalDate promotionEndDate;

    /** Workflow modération : PENDING / APPROVED / REJECTED. */
    private ProductModerationStatus moderationStatus;

    /** Motif de rejet fourni par l'admin (nullable). */
    private String moderationReason;

    /** Dernier log audit produit (avant/après) pour aider l'admin à vérifier les modifications. */
    private String lastChangesBefore;
    private String lastChangesAfter;
    private String lastAuditAction;
    private LocalDateTime lastAuditAt;
}
