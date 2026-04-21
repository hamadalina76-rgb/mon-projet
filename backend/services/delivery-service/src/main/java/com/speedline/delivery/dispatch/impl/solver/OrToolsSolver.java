package com.speedline.delivery.dispatch.impl.solver;

import com.speedline.delivery.dispatch.client.SolverServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.impl.solver.dto.SolveRequest;
import com.speedline.delivery.dispatch.impl.solver.dto.SolveResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DISP-102: OR-Tools client solver. Exposes an async API so Resilience4j TimeLimiter can apply.
 */
@Slf4j
@RequiredArgsConstructor
public class OrToolsSolver {

    private final SolverServiceClient solverServiceClient;
    private final DispatchProperties dispatchProperties;
    private final GreedySolver greedySolver;

    @CircuitBreaker(name = "orToolsSolver", fallbackMethod = "fallbackAsync")
    @TimeLimiter(name = "orToolsSolver")
    @Retry(name = "orToolsSolver")
    public CompletableFuture<List<Assignment>> solveAsync(CostMatrix matrix) {
        return CompletableFuture.supplyAsync(() -> callRemote(matrix));
    }

    public CompletableFuture<List<Assignment>> fallbackAsync(CostMatrix matrix, Throwable throwable) {
        log.warn("OR-Tools unavailable, fallback to Greedy: {}", throwable.getMessage());
        return CompletableFuture.completedFuture(greedySolver.solve(matrix));
    }

    private List<Assignment> callRemote(CostMatrix matrix) {
        SolveRequest request = toRequest(matrix);
        SolveResponse response = solverServiceClient.solve(request);
        return toAssignments(response);
    }

    private SolveRequest toRequest(CostMatrix matrix) {
        final double infCost = configuredInfCost();

        List<PendingOrder> orders = matrix.getOrders() == null ? List.of() : matrix.getOrders();
        List<AvailableCourier> couriers = matrix.getCouriers() == null ? List.of() : matrix.getCouriers();

        List<SolveRequest.OrderNode> orderNodes = new ArrayList<>(orders.size());
        for (PendingOrder o : orders) {
            orderNodes.add(SolveRequest.OrderNode.builder()
                    .id(o.getId())
                    .partnerId(o.getPartnerId())
                    .customerId(o.getCustomerId())
                    .pickupLat(o.getPartnerLat())
                    .pickupLon(o.getPartnerLon())
                    .deliveryLat(o.getCustomerLat())
                    .deliveryLon(o.getCustomerLon())
                    .guaranteedDeliveryMinutes(o.getGuaranteedDeliveryMinutes())
                    .createdAt(o.getCreatedAt())
                    .urgent(o.getIsUrgent())
                    .largeOrder(o.getIsLargeOrder())
                    .scheduled(o.getIsScheduled())
                    .build());
        }

        List<SolveRequest.CourierNode> courierNodes = new ArrayList<>(couriers.size());
        for (AvailableCourier c : couriers) {
            int capacity = c.getMaxCapacity() != null ? c.getMaxCapacity() : capacityFromVehicleType(c.getVehicleType());
            courierNodes.add(SolveRequest.CourierNode.builder()
                    .id(c.getId())
                    .zoneId(c.getZoneId())
                    .lat(c.getLat())
                    .lon(c.getLon())
                    .vehicleType(c.getVehicleType())
                    .capacity(capacity)
                    .build());
        }

        List<List<Double>> costs = new ArrayList<>(orders.size());
        for (PendingOrder o : orders) {
            List<Double> row = new ArrayList<>(couriers.size());
            for (AvailableCourier c : couriers) {
                CostResult cost = matrix.get(o.getId(), c.getId());
                if (cost == null || !Boolean.TRUE.equals(cost.getFeasible()) || cost.getTotalCost() == null) {
                    row.add(infCost);
                } else {
                    row.add(cost.getTotalCost());
                }
            }
            costs.add(row);
        }

        boolean useVrppd = orders.size() > dispatchProperties.getSolver().getHungarianMaxOrders()
                || orders.stream().anyMatch(o -> Boolean.TRUE.equals(o.getIsLargeOrder()));

        return SolveRequest.builder()
                .algorithm(useVrppd ? "VRPPD" : "MCF")
                .orders(orderNodes)
                .couriers(courierNodes)
                .costs(costs)
                .timeoutMs(dispatchProperties.getSolver().getOrtools().getTimeoutMs())
                .build();
    }

    private double configuredInfCost() {
        double configured = dispatchProperties.getSolver().getInfCostPlaceholder();
        return configured > 0 ? configured : HungarianAlgorithm.INF_COST_PLACEHOLDER;
    }

    private List<Assignment> toAssignments(SolveResponse response) {
        if (response == null || response.getAssignments() == null || response.getAssignments().isEmpty()) {
            return List.of();
        }
        List<Assignment> assignments = new ArrayList<>(response.getAssignments().size());
        for (SolveResponse.AssignmentOut item : response.getAssignments()) {
            assignments.add(Assignment.builder()
                    .orderId(item.getOrderId())
                    .courierId(item.getCourierId())
                    .bundleId(item.getBundleId())
                    .cost(item.getCost())
                    .etaPickupMin(item.getEtaPickupMin())
                    .etaDeliveryMin(item.getEtaDeliveryMin())
                    .build());
        }
        return assignments;
    }

    private static int capacityFromVehicleType(String vehicleType) {
        if (vehicleType == null) {
            return 1;
        }
        return switch (vehicleType.toUpperCase()) {
            case "MOTO" -> 2;
            case "MOTOTRICYCLE" -> 4;
            default -> 1;
        };
    }
}
