package com.speedline.promotion.validation.validators;

import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.validation.AbstractPromotionValidator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Maillon 3 — start_date ≤ now ≤ end_date.
 */
@Component
@Order(3)
public class DateRangeValidator extends AbstractPromotionValidator {

    @Override
    public void validate(ValidationContext context) {
        var p   = context.getPromotion();
        var now = LocalDateTime.now();

        if (p.getStartDate() != null && now.isBefore(p.getStartDate())) {
            throw PromotionException.notStarted(p.getCode());
        }
        if (p.getEndDate() != null && now.isAfter(p.getEndDate())) {
            throw PromotionException.expired(p.getCode());
        }
        forward(context);
    }
}
