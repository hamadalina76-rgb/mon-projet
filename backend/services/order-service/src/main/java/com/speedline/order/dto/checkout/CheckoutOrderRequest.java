package com.speedline.order.dto.checkout;

import jakarta.validation.Valid;
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

    @Valid
    private DeliveryAddressRequest deliveryAddressDetails;

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
    public static class DeliveryAddressRequest {

        @Size(max = 100, message = "L'identifiant d'adresse est invalide")
        private String addressId;

        @Size(max = 255, message = "Le label d'adresse ne doit pas dépasser 255 caractères")
        private String label;

        @Size(max = 500, message = "L'adresse de livraison ne doit pas dépasser 500 caractères")
        private String deliveryAddress;

        @Size(max = 500, message = "La localisation de livraison ne doit pas dépasser 500 caractères")
        private String deliveryLocation;

        @Size(max = 255, message = "La rue ne doit pas dépasser 255 caractères")
        private String street;

        @Size(max = 255, message = "Le bâtiment ne doit pas dépasser 255 caractères")
        private String building;

        @Size(max = 50, message = "L'étage ne doit pas dépasser 50 caractères")
        private String floor;

        @Size(max = 50, message = "L'appartement ne doit pas dépasser 50 caractères")
        private String apartment;

        @Size(max = 255, message = "La ville ne doit pas dépasser 255 caractères")
        private String city;

        @Size(max = 50, message = "Le code postal ne doit pas dépasser 50 caractères")
        private String postalCode;

        @Size(max = 255, message = "La région ne doit pas dépasser 255 caractères")
        private String state;

        @Size(max = 255, message = "Le pays ne doit pas dépasser 255 caractères")
        private String country;

        @Size(max = 500, message = "L'adresse formatée ne doit pas dépasser 500 caractères")
        private String formattedAddress;

        private BigDecimal latitude;

        private BigDecimal longitude;

        private BigDecimal deliveryLatitude;

        private BigDecimal deliveryLongitude;

        @Size(max = 500, message = "Les instructions de livraison ne doivent pas dépasser 500 caractères")
        private String deliveryInstructions;

        @Size(max = 255, message = "Le nom du contact ne doit pas dépasser 255 caractères")
        private String contactName;

        @Size(max = 50, message = "Le téléphone du contact ne doit pas dépasser 50 caractères")
        private String contactPhone;

        private Boolean isFromMap;
    }

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
        private List<Object> selectedOptions = List.of();

        @Size(max = 500, message = "La note cuisine ne doit pas dépasser 500 caractères")
        private String kitchenNote;
    }
}
