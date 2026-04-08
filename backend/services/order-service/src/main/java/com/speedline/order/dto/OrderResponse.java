package com.speedline.order.dto;

import com.speedline.order.domain.Order.OrderType;
import com.speedline.order.domain.Order.PaymentMethod;
import com.speedline.order.domain.Order.PaymentStatus;
import com.speedline.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la réponse d'une commande
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    
    // IDs de référence
    private Long customerId;
    private Long partnerId;
    private Long courierId;
    
    // Informations client
    private String customerName;
    private String customerPhone;
    
    // Informations partenaire
    private String partnerName;
    private String partnerAddress;
    private String partnerPhone;
    
    // Informations livreur
    private String courierName;
    private String courierPhone;
    
    // Statut
    private OrderStatus status;
    private String statusLabel;
    private OrderType type;
    
    // Montants
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal serviceFee;
    private BigDecimal tax;
    private BigDecimal discount;
    private String promoCode;
    private BigDecimal tip;
    private BigDecimal total;
    
    // Adresse de livraison
    private DeliveryAddressDTO deliveryAddress;
    private String deliveryInstructions;
    
    // Paiement
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    
    // Temps
    private LocalDateTime orderTime;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    /** Max des temps produit (minutes) à la commande ; utile pour l’acceptation côté partenaire. */
    private Integer suggestedPreparationMinutes;
    private Boolean isScheduled;
    private LocalDateTime scheduledDeliveryTime;
    
    // Notes
    private String customerNotes;
    private String cancellationReason;
    
    // Articles
    private List<OrderItemDTO> items;
    
    // Historique (optionnel, pour détails)
    private List<StatusHistoryDTO> statusHistory;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Métadonnées calculées
    private Integer deliveryTimeMinutes;
    private Boolean isCancellable;
    private Boolean isCompleted;

    /**
     * DTO pour l'adresse de livraison
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAddressDTO {
        private String street;
        private String building;
        private String floor;
        private String apartment;
        private String city;
        private String postalCode;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String formattedAddress;
    }

    /**
     * DTO pour l'historique de statut
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryDTO {
        private OrderStatus status;
        private OrderStatus previousStatus;
        private String description;
        private String notes;
        private String updatedBy;
        private String actorType;
        private LocalDateTime timestamp;
        /** Minutes de préparation indiquées par le partenaire à l'acceptation (si présent). */
        private Integer estimatedPrepMinutes;
    }
}
