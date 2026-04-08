package com.speedline.promotion.validation.validators;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.repository.PromotionRuleRepository;
import com.speedline.promotion.rules.RuleEvaluator;
import com.speedline.promotion.rules.impl.MinOrderRule;
import com.speedline.promotion.validation.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RulesValidatorTest {

    @Mock
    private PromotionRuleRepository ruleRepository;

    private RulesValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RulesValidator(ruleRepository, List.of(new MinOrderRule()));
    }

    @Test
    @DisplayName("Règle MIN_ORDER dynamique non respectée → exception")
    void shouldThrowWhenDynamicMinOrderNotMet() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true).build();

        PromotionRule rule = PromotionRule.builder()
                .promotionId(1L).ruleType(RuleType.MIN_ORDER).targetValue("50").build();
        when(ruleRepository.findByPromotionId(1L)).thenReturn(List.of(rule));

        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(new BigDecimal("30")).build();

        assertThrows(PromotionException.class, () -> validator.validate(ctx));
    }

    @Test
    @DisplayName("Aucune règle → passe")
    void shouldPassWhenNoRules() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true).build();

        when(ruleRepository.findByPromotionId(1L)).thenReturn(List.of());

        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(new BigDecimal("100")).build();

        assertDoesNotThrow(() -> validator.validate(ctx));
    }
}
