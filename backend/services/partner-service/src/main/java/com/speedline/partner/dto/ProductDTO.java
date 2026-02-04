package com.speedline.partner.dto;

import com.speedline.partner.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO pour Product - Utilisé pour les réponses API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Long id;
    private Long partnerId;
    private Long categoryId;
    private String categoryName;
    
    // Informations de base
    private String name;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal discountPercentage;
    private BigDecimal finalPrice;
    
    // Images
    private String image;
    private List<String> images;
    
    // Disponibilité
    private Boolean isAvailable;
    private Integer stockQuantity;
    private ProductStatus status;
    private Integer preparationTime;
    
    // Informations nutritionnelles
    private NutritionalInfoDTO nutritionalInfo;
    private List<String> allergens;
    private List<String> ingredients;
    
    // Caractéristiques
    private Boolean isVegetarian;
    private Boolean isVegan;
    private Boolean isHalal;
    private Boolean isGlutenFree;
    private Integer spicyLevel;
    private Boolean isPopular;
    private Boolean isNew;
    private Boolean isFeatured;
    private Boolean isOnSale;
    
    // Statistiques
    private Integer orderCount;
    private BigDecimal rating;
    private Integer totalRatings;
    
    // Options et addons
    private List<ProductOptionDTO> options;
    private List<ProductAddonDTO> addons;

    /**
     * DTO pour les informations nutritionnelles
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutritionalInfoDTO {
        private Integer calories;
        private Integer protein;
        private Integer carbs;
        private Integer fat;
        private Integer fiber;
        private Integer sodium;
    }
}
