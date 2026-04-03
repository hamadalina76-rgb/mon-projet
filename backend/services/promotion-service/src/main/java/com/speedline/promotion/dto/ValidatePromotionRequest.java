package com.speedline.promotion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ValidatePromotionRequest(

    @NotBlank(message = "Le code est obligatoire")
    String code,

    @NotNull(message = "L'identifiant utilisateur est obligatoire")
    Long userId,

    @NotNull(message = "Le sous-total est obligatoire")
    @DecimalMin(value = "0", message = "Le sous-total ne peut pas être négatif")
    BigDecimal orderSubtotal,

    BigDecimal deliveryFee,

    Long partnerId,

    List<Long> categoryIds,

    List<Long> productIds
) {}
