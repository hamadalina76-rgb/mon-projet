package com.speedline.order.dto;

import com.speedline.order.domain.Order.OrderType;
import com.speedline.order.domain.Order.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour créer une nouvelle commande
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    /**
     * ID du client (obligatoire)
     */
    @NotNull(message = "L'ID du client est obligatoire")
    private Long customerId;

    /**
     * ID du partenaire (obligatoire)
     */
    @NotNull(message = "L'ID du partenaire est obligatoire")
    private Long partnerId;

    /**
     * Articles de la commande (obligatoire, min 1)
     */
    @NotEmpty(message = "La commande doit contenir au moins un article")
    private List<OrderItemRequest> items;

    /**
     * ID de l'adresse de livraison (obligatoire pour DELIVERY)
     */
    private Long deliveryAddressId;

    /**
     * Type de commande (DELIVERY ou PICKUP)
     */
    @Builder.Default
    private OrderType type = OrderType.DELIVERY;

    /**
     * Méthode de paiement (obligatoire)
     */
    @NotNull(message = "La méthode de paiement est obligatoire")
    private PaymentMethod paymentMethod;

    /**
     * ID de la méthode de paiement (pour CARD)
     */
    private Long paymentMethodId;

    /**
     * Code promo (optionnel)
     */
    @Size(max = 50, message = "Le code promo ne doit pas dépasser 50 caractères")
    private String promoCode;

    /**
     * Pourboire pour le livreur
     */
    @Builder.Default
    private BigDecimal tip = BigDecimal.ZERO;

    /**
     * Instructions de livraison
     */
    @Size(max = 500, message = "Les instructions ne doivent pas dépasser 500 caractères")
    private String deliveryInstructions;

    /**
     * Notes du client
     */
    @Size(max = 500, message = "Les notes ne doivent pas dépasser 500 caractères")
    private String customerNotes;

    /**
     * Commande planifiée pour plus tard
     */
    @Builder.Default
    private Boolean isScheduled = false;

    /**
     * Heure de livraison souhaitée (si planifiée)
     */
    private LocalDateTime scheduledDeliveryTime;

    /**
     * DTO pour un article de commande
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        
        /**
         * ID du produit (obligatoire)
         */
        @NotNull(message = "L'ID du produit est obligatoire")
        private Long productId;
        
        /**
         * Quantité (obligatoire, min 1)
         */
        @NotNull(message = "La quantité est obligatoire")
        private Integer quantity;
        
        /**
         * IDs des valeurs d'options sélectionnées
         */
        private List<Long> selectedOptionValueIds;
        
        /**
         * Suppléments sélectionnés (format: addonId:quantity)
         */
        private List<AddonSelection> selectedAddons;
        
        /**
         * Instructions spéciales pour cet article
         */
        @Size(max = 500, message = "Les instructions ne doivent pas dépasser 500 caractères")
        private String specialInstructions;
    }

    /**
     * DTO pour un supplément sélectionné
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddonSelection {
        private Long addonId;
        private Integer quantity;
    }
}
