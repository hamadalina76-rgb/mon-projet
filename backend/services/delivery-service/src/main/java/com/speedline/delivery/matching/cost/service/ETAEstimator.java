package com.speedline.delivery.matching.cost.service;

import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;

public interface ETAEstimator {
    ETAEstimation estimate(ScoringContext context);
}
