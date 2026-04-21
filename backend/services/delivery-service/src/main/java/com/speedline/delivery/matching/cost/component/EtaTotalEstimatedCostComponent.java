package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class EtaTotalEstimatedCostComponent extends BaseCostComponent {

    @Override
    public CostComponentKey key() {
        return CostComponentKey.ETA_TOTAL_ESTIMATED;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        double raw = Math.min(100.0, Math.max(0.0, eta.totalMinutes()));
        return score(config, raw, "ETA totale estimée: " + eta.totalMinutes() + " min");
    }
}
