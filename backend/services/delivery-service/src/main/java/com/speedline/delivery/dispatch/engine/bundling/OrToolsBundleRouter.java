package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.client.SolverServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.impl.solver.dto.SolveRequest;
import com.speedline.delivery.dispatch.impl.solver.dto.SolveResponse;
import com.speedline.delivery.dispatch.util.GeoUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class OrToolsBundleRouter implements BundleRouter {

    private final SolverServiceClient solverServiceClient;
    private final DispatchProperties dispatchProperties;
    private final NearestNeighborRouter nearestNeighborRouter;

    @Override
    @CircuitBreaker(name = "bundle-ortools", fallbackMethod = "fallbackRoute")
    public BundleRoute route(AvailableCourier courier, List<PendingOrder> bundleOrders) {
        SolveRequest request = toSolveRequest(courier, bundleOrders);
        SolveResponse response = solverServiceClient.solve(request);
        if (response == null || response.getAssignments() == null || response.getAssignments().isEmpty()) {
            return nearestNeighborRouter.route(courier, bundleOrders);
        }

        Map<Long, Integer> pickupEta = new HashMap<>();
        Map<Long, Integer> deliveryEta = new HashMap<>();
        List<BundleRoute.Stop> stops = new ArrayList<>();

        for (SolveResponse.AssignmentOut out : response.getAssignments()) {
            if (out.getOrderId() == null) {
                continue;
            }
            if (out.getEtaPickupMin() != null) {
                pickupEta.put(out.getOrderId(), out.getEtaPickupMin());
                PendingOrder order = findOrder(bundleOrders, out.getOrderId());
                if (order != null) {
                    stops.add(BundleRoute.Stop.builder()
                            .orderId(out.getOrderId())
                            .type("PICKUP")
                            .lat(order.getPartnerLat())
                            .lon(order.getPartnerLon())
                            .etaMin(out.getEtaPickupMin())
                            .build());
                }
            }
            if (out.getEtaDeliveryMin() != null) {
                deliveryEta.put(out.getOrderId(), out.getEtaDeliveryMin());
                PendingOrder order = findOrder(bundleOrders, out.getOrderId());
                if (order != null) {
                    stops.add(BundleRoute.Stop.builder()
                            .orderId(out.getOrderId())
                            .type("DROPOFF")
                            .lat(order.getCustomerLat())
                            .lon(order.getCustomerLon())
                            .etaMin(out.getEtaDeliveryMin())
                            .build());
                }
            }
        }

        if (deliveryEta.isEmpty()) {
            return nearestNeighborRouter.route(courier, bundleOrders);
        }

        return BundleRoute.builder()
                .stops(stops)
                .etaPickupMinByOrder(pickupEta)
                .etaDeliveryMinByOrder(deliveryEta)
                .build();
    }

    @SuppressWarnings("unused")
    public BundleRoute fallbackRoute(AvailableCourier courier, List<PendingOrder> bundleOrders, Throwable throwable) {
        log.warn("bundle routing fallback to nearest-neighbor: {}", throwable.getMessage());
        return nearestNeighborRouter.route(courier, bundleOrders);
    }

    private SolveRequest toSolveRequest(AvailableCourier courier, List<PendingOrder> bundleOrders) {
        double inf = dispatchProperties.getSolver().getInfCostPlaceholder();
        if (inf <= 0.0) {
            inf = 1_000_000_000_000.0;
        }

        List<SolveRequest.OrderNode> orders = bundleOrders.stream()
                .map(order -> SolveRequest.OrderNode.builder()
                        .id(order.getId())
                        .partnerId(order.getPartnerId())
                        .customerId(order.getCustomerId())
                        .pickupLat(order.getPartnerLat())
                        .pickupLon(order.getPartnerLon())
                        .deliveryLat(order.getCustomerLat())
                        .deliveryLon(order.getCustomerLon())
                        .guaranteedDeliveryMinutes(order.getGuaranteedDeliveryMinutes())
                        .createdAt(order.getCreatedAt())
                        .urgent(order.getIsUrgent())
                        .largeOrder(order.getIsLargeOrder())
                        .scheduled(order.getIsScheduled())
                        .build())
                .toList();

        SolveRequest.CourierNode courierNode = SolveRequest.CourierNode.builder()
                .id(courier.getId())
                .zoneId(courier.getZoneId())
                .lat(courier.getLat())
                .lon(courier.getLon())
                .vehicleType(courier.getVehicleType())
                .capacity(courier.getMaxCapacity() == null ? 1 : courier.getMaxCapacity())
                .build();

        List<List<Double>> costs = new ArrayList<>(bundleOrders.size());
        for (PendingOrder order : bundleOrders) {
            double cost = inf;
            if (courier.getLat() != null && courier.getLon() != null
                    && order.getPartnerLat() != null && order.getPartnerLon() != null
                    && order.getCustomerLat() != null && order.getCustomerLon() != null) {
                double leg1 = GeoUtil.haversineMeters(courier.getLat(), courier.getLon(), order.getPartnerLat(), order.getPartnerLon());
                double leg2 = GeoUtil.haversineMeters(order.getPartnerLat(), order.getPartnerLon(), order.getCustomerLat(), order.getCustomerLon());
                cost = leg1 + leg2;
            }
            costs.add(List.of(cost));
        }

        return SolveRequest.builder()
                .algorithm("VRPPD")
                .orders(orders)
                .couriers(List.of(courierNode))
                .costs(costs)
                .timeoutMs(dispatchProperties.getSolver().getOrtools().getTimeoutMs())
                .build();
    }

    private PendingOrder findOrder(List<PendingOrder> bundleOrders, Long orderId) {
        for (PendingOrder order : bundleOrders) {
            if (order != null && orderId.equals(order.getId())) {
                return order;
            }
        }
        return null;
    }
}
