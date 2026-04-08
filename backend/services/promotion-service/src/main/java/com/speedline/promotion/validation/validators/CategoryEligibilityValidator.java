package com.speedline.promotion.validation.validators;

import com.speedline.promotion.dto.PromotionDto;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.validation.AbstractPromotionValidator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maillon 8 — catégorie éligible.
 */
@Component
@Order(8)
public class CategoryEligibilityValidator extends AbstractPromotionValidator {

    @Override
    public void validate(ValidationContext context) {
        var p = context.getPromotion();
        if (p.getApplicableCategoryIds() != null && !p.getApplicableCategoryIds().isBlank()
                && context.getCategoryIds() != null && !context.getCategoryIds().isEmpty()) {
            List<Long> allowed = PromotionDto.parseIds(p.getApplicableCategoryIds());
            if (!allowed.isEmpty() && context.getCategoryIds().stream().noneMatch(allowed::contains)) {
                throw PromotionException.categoryNotEligible(p.getCode());
            }
        }
        forward(context);
    }
}
