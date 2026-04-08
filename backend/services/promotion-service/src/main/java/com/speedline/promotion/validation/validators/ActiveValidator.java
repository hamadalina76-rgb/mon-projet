package com.speedline.promotion.validation.validators;

import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.validation.AbstractPromotionValidator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Maillon 2 — la promotion doit être active (is_active = true).
 */
@Component
@Order(2)
public class ActiveValidator extends AbstractPromotionValidator {

    @Override
    public void validate(ValidationContext context) {
        if (!Boolean.TRUE.equals(context.getPromotion().getIsActive())) {
            throw PromotionException.inactive(context.getPromotion().getCode());
        }
        forward(context);
    }
}
