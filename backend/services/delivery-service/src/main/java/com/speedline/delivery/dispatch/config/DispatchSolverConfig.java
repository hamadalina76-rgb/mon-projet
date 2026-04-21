package com.speedline.delivery.dispatch.config;

import com.speedline.delivery.dispatch.client.SolverServiceClient;
import com.speedline.delivery.dispatch.impl.solver.GreedySolver;
import com.speedline.delivery.dispatch.impl.solver.HungarianSolver;
import com.speedline.delivery.dispatch.impl.solver.OrToolsSolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DispatchSolverConfig {

    @Bean
    public GreedySolver greedySolver() {
        return new GreedySolver();
    }

    @Bean
    public HungarianSolver hungarianSolver() {
        return new HungarianSolver();
    }

    @Bean
    public OrToolsSolver orToolsSolver(SolverServiceClient solverServiceClient,
                                       DispatchProperties dispatchProperties,
                                       GreedySolver greedySolver) {
        return new OrToolsSolver(solverServiceClient, dispatchProperties, greedySolver);
    }
}
