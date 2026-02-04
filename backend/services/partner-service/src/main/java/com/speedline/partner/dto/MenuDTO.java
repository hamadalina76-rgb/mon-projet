package com.speedline.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour le menu complet d'un partenaire
 * Organisé par catégories
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuDTO {

    /**
     * ID du partenaire
     */
    private Long partnerId;

    /**
     * Nom du partenaire
     */
    private String partnerName;

    /**
     * Sections du menu (par catégorie)
     */
    private List<MenuSectionDTO> sections;

    /**
     * Produits populaires (tous confondus)
     */
    private List<ProductDTO> popularProducts;

    /**
     * Nouveaux produits
     */
    private List<ProductDTO> newProducts;

    /**
     * Produits en promotion
     */
    private List<ProductDTO> promotionalProducts;

    /**
     * Section du menu (une catégorie avec ses produits)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuSectionDTO {
        private Long categoryId;
        private String categoryName;
        private String categoryDescription;
        private String categoryIcon;
        private Integer displayOrder;
        private List<ProductDTO> products;
    }
}
