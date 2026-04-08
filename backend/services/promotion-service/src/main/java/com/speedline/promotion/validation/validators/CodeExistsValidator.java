package com.speedline.promotion.validation.validators;

import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.validation.AbstractPromotionValidator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Maillon 1 — vérifie simplement que la promotion n'est pas null
 * (le code a été trouvé en base).
 */
@Component
@Order(1)
public class CodeExistsValidator extends AbstractPromotionValidator {

    @Override
    public void validate(ValidationContext context) {
        if (context.getPromotion() == null) {
            throw PromotionException.notFound("UNKNOWN");
        }
        forward(context);
    }
}
