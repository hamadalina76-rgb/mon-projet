package com.speedline.promotion.dto;

import java.math.BigDecimal;

public record SimulateDiscountResponse(
    BigDecimal discountAmount,
    BigDecimal newSubtotal,
    BigDecimal deliveryFee,
    BigDecimal total,
    String message
) {}
