package com.speedline.delivery.matching.cost.service;

import com.speedline.delivery.matching.cost.model.CostResult;
import com.speedline.delivery.matching.cost.model.ScoringContext;

public interface CostFunctionService {
    CostResult calculate(ScoringContext context);
}
