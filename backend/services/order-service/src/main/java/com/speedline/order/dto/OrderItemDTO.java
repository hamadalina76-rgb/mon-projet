package com.speedline.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO pour un article de commande
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    private Long id;
    private Long productId;
    
    // Informations produit
    private String productName;
    private String productDescription;
    private String productImage;
    
    // Quantité et prix
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal modifiersTotal;
    private BigDecimal totalUnitPrice;
    private BigDecimal subtotal;
    
    // Options et suppléments
    private List<SelectedOptionDTO> selectedOptions;
    private List<SelectedAddonDTO> selectedAddons;
    
    // Instructions
    private String specialInstructions;

    /** Minutes de préparation (fiche produit au moment de la commande), si connues. */
    private Integer preparationTimeMin;

    /**
     * DTO pour une option sélectionnée
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedOptionDTO {
        private Long optionId;
        private String optionName;
        private Long valueId;
        private String valueName;
        private BigDecimal priceModifier;
    }

    /**
     * DTO pour un supplément sélectionné
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedAddonDTO {
        private Long addonId;
        private String addonName;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal total;
    }
}
