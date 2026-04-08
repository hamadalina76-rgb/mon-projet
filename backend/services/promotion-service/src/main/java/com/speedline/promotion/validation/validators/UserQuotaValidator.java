package com.speedline.promotion.validation.validators;

import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.repository.UserPromotionRepository;
import com.speedline.promotion.validation.AbstractPromotionValidator;
import com.speedline.promotion.validation.ValidationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Maillon 5 — quota par utilisateur.
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class UserQuotaValidator extends AbstractPromotionValidator {

    private final UserPromotionRepository userPromotionRepository;

    @Override
    public void validate(ValidationContext context) {
        var p = context.getPromotion();
        if (p.getUsageLimitPerUser() != null && context.getUserId() != null) {
            long used = userPromotionRepository
                    .findByUserIdAndPromotionId(context.getUserId(), p.getId())
                    .map(u -> (long) u.getUsageCount())
                    .orElse(0L);
            if (used >= p.getUsageLimitPerUser()) {
                throw PromotionException.userQuotaExceeded(p.getCode());
            }
        }
        forward(context);
    }
}
