package com.speedline.delivery.matching.cost.service;

import com.speedline.delivery.matching.cost.model.CostResult;
import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;
import com.speedline.delivery.matching.cost.model.ScoringContext;

public interface CostFunctionService {
    CostResult calculate(ScoringContext context);

    /**
     * Scoring using an in-memory config snapshot (simulation / replay). Does not read Redis for weights.
     */
    CostResult calculate(ScoringContext context, DispatchConfigSnapshot configOverride);
}
