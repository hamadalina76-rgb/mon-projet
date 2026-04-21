package com.speedline.delivery.matching.cost.service.impl;

import com.speedline.delivery.matching.cost.component.CostComponent;
import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.CostDecision;
import com.speedline.delivery.matching.cost.model.CostResult;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import com.speedline.delivery.matching.cost.service.CostFunctionService;
import com.speedline.delivery.matching.cost.service.DispatchConfigService;
import com.speedline.delivery.matching.cost.service.ETAEstimator;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class CostFunctionServiceImpl implements CostFunctionService {

    private final ETAEstimator etaEstimator;
    private final DispatchConfigService dispatchConfigService;
    private final Map<CostComponentKey, CostComponent> componentsByKey;
    private final DispatchProperties dispatchProperties;

    public CostFunctionServiceImpl(
            ETAEstimator etaEstimator,
            DispatchConfigService dispatchConfigService,
            List<CostComponent> components
    ) {
        this(etaEstimator, dispatchConfigService, components, new DispatchProperties());
    }

    @Autowired
    public CostFunctionServiceImpl(
            ETAEstimator etaEstimator,
            DispatchConfigService dispatchConfigService,
            List<CostComponent> components,
            DispatchProperties dispatchProperties
    ) {
        this.etaEstimator = etaEstimator;
        this.dispatchConfigService = dispatchConfigService;
        this.dispatchProperties = dispatchProperties;
        this.componentsByKey = new EnumMap<>(CostComponentKey.class);
        components.forEach(component -> componentsByKey.put(component.key(), component));
    }

    @Override
    public CostResult calculate(ScoringContext context) {
        DispatchConfigSnapshot snapshot = dispatchConfigService.getCurrentConfig();
        ETAEstimation eta = etaEstimator.estimate(context);

        CostResult.CostResultBuilder builder = CostResult.builder()
                .decision(CostDecision.PASSED)
                .eta(eta);

        double total = 0.0;

        for (DispatchComponentConfig componentConfig : snapshot.orderedComponents()) {
            if (!componentConfig.isEnabled() && !componentConfig.isMandatory()) {
                continue;
            }

            CostComponent component = componentsByKey.get(componentConfig.getKey());
            if (component == null) {
                continue;
            }

            CostContribution contribution = component.evaluate(context, componentConfig, eta);
            builder.contribution(contribution);

            if (contribution.isEliminated()) {
                return builder
                        .decision(CostDecision.ELIMINATED)
                        .eliminationReason(contribution.getEliminationReason())
                    .totalCost(dispatchProperties.getMatching().getInfiniteCost())
                        .build();
            }

            total += contribution.getWeightedCost();
        }

        return builder.totalCost(total).build();
    }
}
