package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class TourCompatibilityCostComponent extends BaseCostComponent {

    @Override
    public CostComponentKey key() {
        return CostComponentKey.TOUR_COMPATIBILITY;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        if (!context.isCourierOnMission()
                || context.getCurrentRouteDropoffLatitude() == null
                || context.getCurrentRouteDropoffLongitude() == null
                || context.getCourierLatitude() == null
                || context.getCourierLongitude() == null) {
            return score(config, 0.0, "Pas de tournée active compatible");
        }

        double ax = context.getCurrentRouteDropoffLongitude().doubleValue() - context.getCourierLongitude().doubleValue();
        double ay = context.getCurrentRouteDropoffLatitude().doubleValue() - context.getCourierLatitude().doubleValue();
        double bx = context.getDropoffLongitude().doubleValue() - context.getPickupLongitude().doubleValue();
        double by = context.getDropoffLatitude().doubleValue() - context.getPickupLatitude().doubleValue();

        double similarity = cosineSimilarity(ax, ay, bx, by);
        double raw = similarity > 0.7 ? -40.0 : 0.0;
        return score(config, raw, similarity > 0.7 ? "Bonus tournée compatible" : "Tournée non compatible");
    }

    private double cosineSimilarity(double ax, double ay, double bx, double by) {
        double dot = ax * bx + ay * by;
        double normA = Math.sqrt(ax * ax + ay * ay);
        double normB = Math.sqrt(bx * bx + by * by);
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (normA * normB);
    }
}
