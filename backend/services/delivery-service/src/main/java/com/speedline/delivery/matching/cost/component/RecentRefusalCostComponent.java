package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class RecentRefusalCostComponent extends BaseCostComponent {

    @Override
    public CostComponentKey key() {
        return CostComponentKey.RECENT_REFUSAL;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        LocalDateTime lastRefusalAt = context.getLastRefusalAt();
        if (lastRefusalAt == null) {
            return score(config, 0.0, "Aucun refus récent");
        }

        long minutes = Duration.between(lastRefusalAt, LocalDateTime.now()).toMinutes();
        if (minutes >= 0 && minutes < 60) {
            return score(config, 20.0, "Refus récent < 1h");
        }
        return score(config, 0.0, "Pas de refus < 1h");
    }
}
