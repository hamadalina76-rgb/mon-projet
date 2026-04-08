package com.speedline.promotion.service;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates the discount amount depending on the promotion type.
 * <ul>
 *   <li>PERCENTAGE  : min(subtotal × value / 100, maximum_discount)</li>
 *   <li>FIXED_AMOUNT: min(value, subtotal)</li>
 *   <li>FREE_DELIVERY: delivery_fee</li>
 * </ul>
 */
@Component
public class DiscountCalculator {

    public BigDecimal compute(Promotion promotion, BigDecimal subtotal, BigDecimal deliveryFee) {
        BigDecimal discount;

        if (promotion.getType() == PromotionType.PERCENTAGE) {
            discount = subtotal
                    .multiply(promotion.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (promotion.getType() == PromotionType.FIXED_AMOUNT) {
            discount = promotion.getValue().min(subtotal);
        } else { // FREE_DELIVERY
            discount = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        }

        // Apply maximum discount cap
        if (promotion.getMaximumDiscount() != null
                && discount.compareTo(promotion.getMaximumDiscount()) > 0) {
            discount = promotion.getMaximumDiscount();
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }
}
