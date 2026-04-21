package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PerformanceRatingCostComponent extends BaseCostComponent {

    private final DispatchProperties dispatchProperties;

    public PerformanceRatingCostComponent() {
        this(new DispatchProperties());
    }

    @Autowired
    public PerformanceRatingCostComponent(DispatchProperties dispatchProperties) {
        this.dispatchProperties = dispatchProperties;
    }

    @Override
    public CostComponentKey key() {
        return CostComponentKey.PERFORMANCE_RATING;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        BigDecimal rating = context.getRating();
        if (rating == null) {
            return score(config, dispatchProperties.getMatching().getPerformanceMissingRatingPenalty(), "Rating absent, légère pénalité");
        }

        if (rating.compareTo(BigDecimal.valueOf(dispatchProperties.getMatching().getPerformanceBelowThreshold())) >= 0) {
            return score(config, 0.0, "Rating >= 4.0");
        }

        double diff = dispatchProperties.getMatching().getPerformanceBelowThreshold() - rating.doubleValue();
        double raw = Math.min(100.0, diff * dispatchProperties.getMatching().getPerformancePenaltyPerPoint());
        return score(config, raw, "Pénalité rating bas");
    }
}
