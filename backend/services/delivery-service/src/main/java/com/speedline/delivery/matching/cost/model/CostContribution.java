package com.speedline.delivery.matching.cost.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CostContribution {
    CostComponentKey key;
    boolean enabled;
    int weight;
    double rawCost;
    double weightedCost;
    boolean eliminated;
    EliminationReason eliminationReason;
    String note;
}
