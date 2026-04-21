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
public class WorkloadFairnessCostComponent extends BaseCostComponent {

    @Override
    public CostComponentKey key() {
        return CostComponentKey.WORKLOAD_FAIRNESS;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        LocalDateTime lastCompleted = context.getLastCompletedDeliveryAt();
        if (lastCompleted == null) {
            return score(config, -10.0, "Aucune livraison récente, bonus équité");
        }

        long minutes = Duration.between(lastCompleted, LocalDateTime.now()).toMinutes();
        if (minutes > 90) {
            return score(config, -10.0, "Inactif depuis longtemps, bonus équité");
        }
        return score(config, 0.0, "Équité neutre");
    }
}
