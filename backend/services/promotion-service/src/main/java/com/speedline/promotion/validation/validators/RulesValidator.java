package com.speedline.promotion.validation.validators;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.repository.PromotionRuleRepository;
import com.speedline.promotion.rules.RuleEvaluator;
import com.speedline.promotion.validation.AbstractPromotionValidator;
import com.speedline.promotion.validation.ValidationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maillon 9 — délègue chaque règle dynamique à son RuleEvaluator (Strategy).
 */
@Component
@Order(9)
@Slf4j
public class RulesValidator extends AbstractPromotionValidator {

    private final PromotionRuleRepository ruleRepository;
    private final Map<RuleType, RuleEvaluator> evaluators;

    public RulesValidator(PromotionRuleRepository ruleRepository, List<RuleEvaluator> evaluatorList) {
        this.ruleRepository = ruleRepository;
        this.evaluators = evaluatorList.stream()
                .collect(Collectors.toMap(RuleEvaluator::supportedType, Function.identity()));
    }

    @Override
    public void validate(ValidationContext context) {
        List<PromotionRule> rules = ruleRepository.findByPromotionId(context.getPromotion().getId());
        for (PromotionRule rule : rules) {
            RuleEvaluator evaluator = evaluators.get(rule.getRuleType());
            if (evaluator != null) {
                evaluator.evaluate(rule, context);
            } else {
                log.warn("No evaluator registered for rule type: {}", rule.getRuleType());
            }
        }
        forward(context);
    }
}
