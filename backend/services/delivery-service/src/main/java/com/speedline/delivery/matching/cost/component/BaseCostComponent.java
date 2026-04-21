package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.EliminationReason;

public abstract class BaseCostComponent implements CostComponent {

    protected CostContribution score(DispatchComponentConfig config, double rawCost, String note) {
        double weighted = rawCost * (config.getWeight() / 10.0);
        return CostContribution.builder()
                .key(config.getKey())
                .enabled(config.isEnabled())
                .weight(config.getWeight())
                .rawCost(rawCost)
                .weightedCost(weighted)
                .eliminated(false)
                .note(note)
                .build();
    }

    protected CostContribution eliminate(DispatchComponentConfig config, EliminationReason reason, String note) {
        return CostContribution.builder()
                .key(config.getKey())
                .enabled(config.isEnabled())
                .weight(config.getWeight())
                .rawCost(0)
                .weightedCost(0)
                .eliminated(true)
                .eliminationReason(reason)
                .note(note)
                .build();
    }
}
