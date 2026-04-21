package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class CourierTypeCostComponent extends BaseCostComponent {

    @Override
    public CostComponentKey key() {
        return CostComponentKey.COURIER_TYPE;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        boolean external = context.getCourierType() != null && "EXTERNAL".equalsIgnoreCase(context.getCourierType());
        double raw = external ? 50.0 : 0.0;
        return score(config, raw, external ? "Pénalité livreur externe" : "Livreur interne");
    }
}
