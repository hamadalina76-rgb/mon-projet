package com.speedline.delivery.matching.cost.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CostResult {
    CostDecision decision;
    EliminationReason eliminationReason;
    double totalCost;
    ETAEstimation eta;

    @Singular
    List<CostContribution> contributions;

    public boolean isEliminated() {
        return decision == CostDecision.ELIMINATED;
    }
}
