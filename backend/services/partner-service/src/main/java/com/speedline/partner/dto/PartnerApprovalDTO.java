package com.speedline.partner.dto;

import com.speedline.partner.domain.CommissionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO pour l'approbation d'un partenaire avec configuration de commission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerApprovalDTO {
    
    /**
     * Type de commission (PERCENTAGE ou MARKUP)
     */
    @NotNull(message = "Commission type is required")
    private CommissionType commissionType;
    
    /**
     * Taux de commission (0.1% à 100%)
     */
    @NotNull(message = "Commission rate is required")
    @DecimalMin(value = "0.1", message = "Commission rate must be at least 0.1%")
    @DecimalMax(value = "100.0", message = "Commission rate cannot exceed 100%")
    private BigDecimal commissionRate;
    
    /**
     * ID de la catégorie principale assignée au partenaire
     */
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    /**
     * IDs des sous-catégories sélectionnées
     */
    private List<Long> subcategoryIds;

    /**
     * Si true : le partenaire peut modifier/créer ses produits sans validation admin.
     * (Auto-approval des modifs sur menu produits)
     */
    private Boolean allowProductUpdatesWithoutApproval;

    /**
     * IDs des zones à assigner lors de l'approbation.
     * Si null: ne modifie pas les zones.
     */
    private List<Long> zoneIds;
}