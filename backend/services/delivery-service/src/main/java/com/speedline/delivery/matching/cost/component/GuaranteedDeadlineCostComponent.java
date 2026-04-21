package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.EliminationReason;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class GuaranteedDeadlineCostComponent extends BaseCostComponent {

    @Override
    public CostComponentKey key() {
        return CostComponentKey.GUARANTEED_DEADLINE;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        Integer guaranteedDelay = context.getGuaranteedDelayMinutes();
        if (guaranteedDelay != null && eta.totalMinutes() > guaranteedDelay) {
            return eliminate(config, EliminationReason.GUARANTEED_DELAY_EXCEEDED,
                    "ETA " + eta.totalMinutes() + " > délai garanti " + guaranteedDelay);
        }
        return score(config, 0.0, "Délai garanti respecté");
    }
}
