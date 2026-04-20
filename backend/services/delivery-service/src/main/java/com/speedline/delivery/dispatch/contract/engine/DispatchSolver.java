package com.speedline.delivery.dispatch.contract.engine;

import com.speedline.delivery.dispatch.contract.model.Assignment;

import java.util.List;

/**
 * Shared contract that produces final assignments from a precomputed matrix.
 * Contract-Version: 1.0
 */
public interface DispatchSolver {

    List<Assignment> solve(CostMatrix matrix);
}
