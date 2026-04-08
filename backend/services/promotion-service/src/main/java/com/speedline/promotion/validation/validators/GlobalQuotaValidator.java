package com.speedline.promotion.validation.validators;

import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.service.RedisQuotaService;
import com.speedline.promotion.validation.AbstractPromotionValidator;
import com.speedline.promotion.validation.ValidationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Maillon 4 — quota global non atteint.
 * Utilise Redis pour une vérification rapide (lecture seule).
 * L'opération DECR atomique n'est faite que dans apply().
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class GlobalQuotaValidator extends AbstractPromotionValidator {

    private final RedisQuotaService redisQuotaService;

    @Override
    public void validate(ValidationContext context) {
        var p = context.getPromotion();
        if (p.getUsageLimit() != null) {
            long remaining = redisQuotaService.remaining(p.getId(), p.getUsageLimit(), p.getUsageCount());
            if (remaining <= 0) {
                throw PromotionException.quotaExceeded(p.getCode());
            }
        }
        forward(context);
    }
}
