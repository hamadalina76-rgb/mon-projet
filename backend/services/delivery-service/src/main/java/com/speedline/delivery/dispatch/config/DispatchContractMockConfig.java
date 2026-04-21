package com.speedline.delivery.dispatch.config;

import com.speedline.delivery.dispatch.contract.engine.BundlingEngine;
import com.speedline.delivery.dispatch.contract.engine.CostFunction;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.mock.MockBundlingEngine;
import com.speedline.delivery.dispatch.contract.mock.MockCostFunction;
import com.speedline.delivery.dispatch.contract.mock.MockDispatchSolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DispatchContractMockConfig {

    @Bean
    @ConditionalOnMissingBean(CostFunction.class)
    public CostFunction costFunction(DispatchProperties properties) {
        return new MockCostFunction(properties.getPreAssignment().getCostPenalty());
    }

    @Bean
    @ConditionalOnMissingBean(DispatchSolver.class)
    public DispatchSolver dispatchSolver() {
        return new MockDispatchSolver();
    }

    @Bean
    @ConditionalOnMissingBean(BundlingEngine.class)
    @Profile("test")
    public BundlingEngine bundlingEngine() {
        return new MockBundlingEngine();
    }
}
