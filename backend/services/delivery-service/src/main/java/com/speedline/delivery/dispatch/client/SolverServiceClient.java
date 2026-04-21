package com.speedline.delivery.dispatch.client;

import com.speedline.delivery.dispatch.impl.bundling.BundleRouteRequest;
import com.speedline.delivery.dispatch.impl.bundling.BundleRouteResponse;
import com.speedline.delivery.dispatch.impl.solver.dto.SolveRequest;
import com.speedline.delivery.dispatch.impl.solver.dto.SolveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "solver-service", url = "${dispatch.solver.ortools.url}")
public interface SolverServiceClient {

    @GetMapping("/health")
    Map<String, Object> health();

    @PostMapping("/solve")
    SolveResponse solve(@RequestBody SolveRequest request);

    @PostMapping("/optimize-route")
    BundleRouteResponse optimizeRoute(@RequestBody BundleRouteRequest request);
}
