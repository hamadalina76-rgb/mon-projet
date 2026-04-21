package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityCostComponent extends BaseCostComponent {

    private final DispatchProperties dispatchProperties;

    public AvailabilityCostComponent() {
        this(new DispatchProperties());
    }

    @Autowired
    public AvailabilityCostComponent(DispatchProperties dispatchProperties) {
        this.dispatchProperties = dispatchProperties;
    }

    @Override
    public CostComponentKey key() {
        return CostComponentKey.AVAILABILITY;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        double raw = 0.0;
        String note = "Livreur disponible";
        if (!context.isCourierAvailable()) {
            raw += dispatchProperties.getMatching().getAvailabilityUnavailablePenalty();
            note = "Livreur indisponible";
        }
        if (context.isCourierOnMission()) {
            raw += dispatchProperties.getMatching().getAvailabilityOnMissionPenalty();
            note = "Livreur déjà en mission";
        }
        return score(config, Math.min(100.0, raw), note);
    }
}
