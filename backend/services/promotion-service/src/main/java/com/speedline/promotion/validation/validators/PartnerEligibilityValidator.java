package com.speedline.promotion.validation.validators;

import com.speedline.promotion.dto.PromotionDto;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.validation.AbstractPromotionValidator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maillon 7 — partenaire éligible.
 */
@Component
@Order(7)
public class PartnerEligibilityValidator extends AbstractPromotionValidator {

    @Override
    public void validate(ValidationContext context) {
        var p = context.getPromotion();
        if (p.getApplicablePartnerIds() != null && !p.getApplicablePartnerIds().isBlank()
                && context.getPartnerId() != null) {
            List<Long> allowed = PromotionDto.parseIds(p.getApplicablePartnerIds());
            if (!allowed.isEmpty() && !allowed.contains(context.getPartnerId())) {
                throw PromotionException.partnerNotEligible(p.getCode());
            }
        }
        forward(context);
    }
}
