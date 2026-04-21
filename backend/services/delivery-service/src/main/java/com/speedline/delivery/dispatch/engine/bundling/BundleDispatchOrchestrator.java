package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.contract.engine.BundlingResult;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BundleDispatchOrchestrator {

    private final BundleAssigner bundleAssigner;
    private final BundleRouter bundleRouter;
    private final DispatchMetrics dispatchMetrics;

    public BundleAssignmentBatch dispatchBundles(BundlingResult bundling,
                                                 Map<Long, PendingOrder> orderById,
                                                 List<AvailableCourier> allCouriers,
                                                 List<AvailableCourier> internalOnlyCouriers,
                                                 Long zoneId) {
        if (bundling == null || bundling.getBundleToOrders() == null || bundling.getBundleToOrders().isEmpty()) {
            return BundleAssignmentBatch.builder()
                    .assignments(List.of())
                    .remainingOrders(new ArrayList<>(orderById.values()))
                    .remainingCouriers(allCouriers)
                    .build();
        }

        List<Assignment> assignments = new ArrayList<>();
        Set<Long> assignedOrderIds = new HashSet<>();
        Set<Long> usedCourierIds = new HashSet<>();
        List<AvailableCourier> availableInternals = new ArrayList<>(internalOnlyCouriers == null ? List.of() : internalOnlyCouriers);

        for (Map.Entry<Long, List<Long>> entry : bundling.getBundleToOrders().entrySet()) {
            Long bundleId = entry.getKey();
            List<Long> memberIds = entry.getValue() == null ? List.of() : entry.getValue();
            List<PendingOrder> members = memberIds.stream()
                    .map(orderById::get)
                    .filter(o -> o != null && o.getId() != null && !assignedOrderIds.contains(o.getId()))
                    .toList();

            if (members.size() < 2) {
                continue;
            }

            AvailableCourier courier = bundleAssigner.pickCourier(members, availableInternals).orElse(null);
            if (courier == null) {
                dispatchMetrics.recordBundlingSkippedNoInternals(zoneId);
                continue;
            }

            BundleRoute route = bundleRouter.route(courier, members);
            for (PendingOrder member : members) {
                assignments.add(Assignment.builder()
                        .orderId(member.getId())
                        .courierId(courier.getId())
                        .bundleId(bundleId)
                        .cost(0.0)
                        .etaPickupMin(route.etaPickupMinByOrder().get(member.getId()))
                        .etaDeliveryMin(route.etaDeliveryMinByOrder().get(member.getId()))
                        .build());
                assignedOrderIds.add(member.getId());
            }

            usedCourierIds.add(courier.getId());
            availableInternals.removeIf(c -> c.getId() != null && c.getId().equals(courier.getId()));
            dispatchMetrics.recordBundleSize(members.size());
        }

        if (!assignments.isEmpty()) {
            dispatchMetrics.recordBundlesCreated(zoneId, bundling.getBundleToOrders().size());
        }

        List<PendingOrder> remainingOrders = orderById.values().stream()
                .filter(order -> order.getId() == null || !assignedOrderIds.contains(order.getId()))
                .toList();

        List<AvailableCourier> remainingCouriers = allCouriers.stream()
                .filter(courier -> courier.getId() == null || !usedCourierIds.contains(courier.getId()))
                .toList();

        return BundleAssignmentBatch.builder()
                .assignments(assignments)
                .remainingOrders(remainingOrders)
                .remainingCouriers(remainingCouriers)
                .build();
    }
}
