package com.speedline.promotion.dto;

import com.speedline.promotion.domain.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SimulateDiscountRequest(
    @NotNull(message = "Le type est obligatoire")
    PromotionType type,

    BigDecimal value,

    BigDecimal maximumDiscount,

    BigDecimal minimumOrder,

    @NotNull(message = "Le sous-total est obligatoire")
    @DecimalMin(value = "0", message = "Le sous-total ne peut pas être négatif")
    BigDecimal orderSubtotal,

    BigDecimal deliveryFee,

    Integer itemCount
) {}
