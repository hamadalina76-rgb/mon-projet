package com.speedline.delivery.service;

import com.speedline.delivery.dispatch.client.SolverServiceClient;
import com.speedline.delivery.dispatch.impl.bundling.BundleRouteRequest;
import com.speedline.delivery.dispatch.impl.bundling.BundleRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Délègue l'optimisation d'itinéraire bundle au solver OR-Tools (HTTP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteOptimizationService {

    private final SolverServiceClient solverServiceClient;

    public BundleRouteResponse optimizeRoute(BundleRouteRequest request) {
        try {
            return solverServiceClient.optimizeRoute(request);
        } catch (Exception ex) {
            log.debug("OR-Tools optimize-route indisponible: {}", ex.getMessage());
            return BundleRouteResponse.builder().build();
        }
    }
}
