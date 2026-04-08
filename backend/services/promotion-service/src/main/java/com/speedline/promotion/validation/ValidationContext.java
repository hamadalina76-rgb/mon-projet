package com.speedline.promotion.validation;

import com.speedline.promotion.domain.Promotion;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ValidationContext {

    private final Promotion promotion;
    private final Long userId;
    private final BigDecimal orderSubtotal;
    private final BigDecimal deliveryFee;
    private final Long partnerId;
    private final List<Long> categoryIds;
    private final Integer itemCount;
}
