package com.speedline.order.dto.checkout;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutOrderRequest {

    @Builder.Default
    private List<CartItemRequest> cartItems = List.of();

    @Size(max = 50, message = "Le code promo ne doit pas dépasser 50 caractères")
    private String promoCode;

    @Size(max = 100, message = "L'identifiant d'adresse est invalide")
    private String addressId;

    @NotBlank(message = "La méthode de paiement est obligatoire")
    private String paymentMethod;

    @Builder.Default
    private Boolean isScheduled = false;

    private LocalDate scheduledDate;

    private LocalTime scheduledTime;

    private LocalDateTime scheduledDeliveryTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemRequest {

        @NotBlank(message = "L'identifiant produit est obligatoire")
        private String productId;

        @NotBlank(message = "L'identifiant partenaire est obligatoire")
        private String partnerId;

        private String productName;

        @NotNull(message = "La quantité est obligatoire")
        @Min(value = 1, message = "La quantité doit être supérieure à 0")
        private Integer quantity;

        private BigDecimal unitPrice;

        @Builder.Default
        private List<String> selectedOptions = List.of();

        @Size(max = 500, message = "La note cuisine ne doit pas dépasser 500 caractères")
        private String kitchenNote;
    }
}
