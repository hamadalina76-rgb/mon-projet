package com.speedline.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO pour la modification d'un partenaire par un administrateur.
 * Tous les champs sont optionnels (null = pas de changement).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPartnerUpdateDTO {

    /** Nom commercial (ex: "Pizza Palace") */
    private String businessName;

    /** Nom de marque / enseigne */
    private String brandName;

    /** Email de contact */
    private String email;

    /** Numéro de téléphone */
    private String phoneNumber;

    /** Type (RESTAURANT, FAST_FOOD, CAFE, BAKERY, GROCERY, PHARMACY, FLORIST, OTHER) */
    private String type;

    /** Ville */
    private String city;

    /** Adresse complète */
    private String address;

    /** Description */
    private String description;

    // ── Commission ────────────────────────────────────────────────────────────

    /** Type de commission : PERCENTAGE ou MARKUP (null = pas de changement) */
    private String commissionType;

    /** Taux de commission en % (ex: 15.5) (null = pas de changement) */
    private BigDecimal commissionRate;

    // ── Catégories ────────────────────────────────────────────────────────────

    /** ID de la catégorie principale (null = pas de changement) */
    private Long categoryId;

    /** IDs des sous-catégories (null = pas de changement, liste vide = retirer toutes) */
    private List<Long> subcategoryIds;

    /**
     * Si true : le partenaire peut modifier/créer ses produits sans validation admin.
     * Si null : ne modifie pas (comportement "partial update").
     */
    private Boolean allowProductUpdatesWithoutApproval;
}
