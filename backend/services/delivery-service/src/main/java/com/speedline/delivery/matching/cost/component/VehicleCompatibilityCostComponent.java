package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.EliminationReason;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class VehicleCompatibilityCostComponent extends BaseCostComponent {

    @Override
    public CostComponentKey key() {
        return CostComponentKey.VEHICLE_COMPATIBILITY;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        if (!context.isBulkyOrder()) {
            return score(config, 0.0, "Commande non volumineuse");
        }

        String type = context.getVehicleType() != null ? context.getVehicleType().toUpperCase() : "";
        boolean compatible = "MOTOR_TRICYCLE".equals(type) || "TRICYCLE".equals(type);
        if (!compatible) {
            return eliminate(config, EliminationReason.VEHICLE_INCOMPATIBLE,
                    "Commande volumineuse, véhicule incompatible: " + type);
        }
        return score(config, 0.0, "Véhicule compatible volumineux");
    }
}
