package com.speedline.promotion.validation.validators;

import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.validation.AbstractPromotionValidator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Maillon 6 — montant minimum de commande.
 */
@Component
@Order(6)
public class MinimumOrderValidator extends AbstractPromotionValidator {

    @Override
    public void validate(ValidationContext context) {
        var p = context.getPromotion();
        if (p.getMinimumOrder() != null
                && context.getOrderSubtotal().compareTo(p.getMinimumOrder()) < 0) {
            throw PromotionException.minimumOrderNotMet(p.getCode());
        }
        forward(context);
    }
}
