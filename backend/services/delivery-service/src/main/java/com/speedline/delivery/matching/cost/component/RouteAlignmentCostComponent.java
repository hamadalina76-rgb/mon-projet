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
public class RouteAlignmentCostComponent extends BaseCostComponent {

    private final DispatchProperties dispatchProperties;

    public RouteAlignmentCostComponent() {
        this(new DispatchProperties());
    }

    @Autowired
    public RouteAlignmentCostComponent(DispatchProperties dispatchProperties) {
        this.dispatchProperties = dispatchProperties;
    }

    @Override
    public CostComponentKey key() {
        return CostComponentKey.ROUTE_ALIGNMENT;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        if (context.getCourierLatitude() == null || context.getCourierLongitude() == null) {
            return score(config, 0.0, "Pas de position live, composante neutre");
        }

        double ax = context.getPickupLongitude().doubleValue() - context.getCourierLongitude().doubleValue();
        double ay = context.getPickupLatitude().doubleValue() - context.getCourierLatitude().doubleValue();
        double bx = context.getDropoffLongitude().doubleValue() - context.getPickupLongitude().doubleValue();
        double by = context.getDropoffLatitude().doubleValue() - context.getPickupLatitude().doubleValue();

        double similarity = cosineSimilarity(ax, ay, bx, by);
        double raw = similarity > 0 ? -dispatchProperties.getMatching().getRouteAlignmentBonusFactor() * similarity : 0.0;
        return score(config, raw, "Alignement directionnel=" + String.format("%.2f", similarity));
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
