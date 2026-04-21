package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.engine.BundlingEngine;
import com.speedline.delivery.dispatch.contract.engine.BundlingResult;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.dispatch.util.GeoUtil;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Service
@Primary
@RequiredArgsConstructor
public class DefaultBundlingEngine implements BundlingEngine {

    private final DispatchProperties dispatchProperties;
    private final BundleFeasibilityChecker feasibilityChecker;
    private final DispatchMetrics dispatchMetrics;

    @Override
    public BundlingResult detectBundles(List<PendingOrder> orders) {
        Instant startedAt = Instant.now();

        try {
            if (!dispatchProperties.getBundling().isEnabled() || orders == null || orders.size() < 2) {
                return BundlingResult.builder().build();
            }

            List<PendingOrder> candidates = orders.stream()
                    .filter(Objects::nonNull)
                    .filter(this::canBeBundled)
                    .filter(this::hasCoordinates)
                    .sorted(Comparator.comparing(PendingOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(PendingOrder::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            if (candidates.size() < 2) {
                return BundlingResult.builder().build();
            }

            Map<Long, PendingOrder> byId = new HashMap<>();
            for (PendingOrder order : candidates) {
                if (order.getId() != null) {
                    byId.put(order.getId(), order);
                }
            }

            List<Set<Long>> pools = buildMergedPartnerPools(candidates);
            List<List<Long>> bundles = new ArrayList<>();
            Map<Long, Long> orderToBundle = new HashMap<>();
            Map<Long, List<Long>> bundleToOrders = new LinkedHashMap<>();
            Map<Long, Integer> etaByOrder = new HashMap<>();
            long nextBundleId = 1L;

            for (Set<Long> pool : pools) {
                List<PendingOrder> remaining = pool.stream()
                        .map(byId::get)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(PendingOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(PendingOrder::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

                while (remaining.size() >= 2) {
                    PendingOrder seed = remaining.get(0);
                    List<PendingOrder> members = new ArrayList<>();
                    members.add(seed);

                    List<PendingOrder> candidatesForSeed = remaining.stream()
                            .skip(1)
                            .sorted(Comparator.comparingDouble(o -> dropoffDistance(seed, o)))
                            .toList();

                    for (PendingOrder candidate : candidatesForSeed) {
                        if (members.size() >= dispatchProperties.getBundling().getMaxBundleSize()) {
                            dispatchMetrics.recordBundlingInfeasibility("max_size");
                            break;
                        }

                        if (dropoffDistance(seed, candidate) > dispatchProperties.getBundling().getDropoffRadiusMeters()) {
                            dispatchMetrics.recordBundlingInfeasibility("radius");
                            continue;
                        }

                        if (!inTimeWindow(members.get(0), candidate)) {
                            dispatchMetrics.recordBundlingInfeasibility("time_window");
                            continue;
                        }

                        List<PendingOrder> tentative = new ArrayList<>(members);
                        tentative.add(candidate);
                        BundleFeasibilityChecker.FeasibilityResult feasibility = feasibilityChecker.isFeasible(
                                tentative,
                                dispatchProperties.getBundling().getAverageSpeedKmh());

                        if (!feasibility.feasible()) {
                            dispatchMetrics.recordBundlingInfeasibility(feasibility.reason());
                            continue;
                        }

                        members = tentative;
                    }

                    if (members.size() >= 2) {
                        BundleFeasibilityChecker.FeasibilityResult feasibility = feasibilityChecker.isFeasible(
                                members,
                                dispatchProperties.getBundling().getAverageSpeedKmh());
                        if (feasibility.feasible()) {
                            long bundleId = nextBundleId++;
                            List<Long> memberIds = members.stream().map(PendingOrder::getId).toList();
                            bundles.add(memberIds);
                            bundleToOrders.put(bundleId, memberIds);
                            for (Long memberId : memberIds) {
                                orderToBundle.put(memberId, bundleId);
                                Integer eta = feasibility.perOrderEtaMin().get(memberId);
                                if (eta != null) {
                                    etaByOrder.put(memberId, eta);
                                }
                            }
                        }
                    }

                    List<Long> taken = members.stream().map(PendingOrder::getId).toList();
                    remaining.removeIf(order -> order.getId() != null && taken.contains(order.getId()));
                    if (members.size() == 1 && !remaining.isEmpty() && remaining.get(0).getId().equals(seed.getId())) {
                        remaining.remove(0);
                    }
                }
            }

            return BundlingResult.builder()
                    .bundles(bundles)
                    .orderToBundleId(orderToBundle)
                    .bundleToOrders(bundleToOrders)
                    .orderEtaMinutes(etaByOrder)
                    .build();
        } finally {
            dispatchMetrics.recordBundlingDuration(Duration.between(startedAt, Instant.now()));
        }
    }

    private boolean canBeBundled(PendingOrder order) {
        if (Boolean.TRUE.equals(order.getIsScheduled()) || Boolean.TRUE.equals(order.getIsLargeOrder())) {
            return false;
        }
        if (dispatchProperties.getBundling().isRespectUrgentFlag() && Boolean.TRUE.equals(order.getIsUrgent())) {
            dispatchMetrics.recordBundlingInfeasibility("urgent");
            return false;
        }
        return true;
    }

    private boolean hasCoordinates(PendingOrder order) {
        return order.getPartnerLat() != null && order.getPartnerLon() != null
                && order.getCustomerLat() != null && order.getCustomerLon() != null;
    }

    private boolean inTimeWindow(PendingOrder earliest, PendingOrder candidate) {
        if (earliest.getCreatedAt() == null || candidate.getCreatedAt() == null) {
            return true;
        }
        long deltaSeconds = Math.abs(candidate.getCreatedAt().getEpochSecond() - earliest.getCreatedAt().getEpochSecond());
        return deltaSeconds <= dispatchProperties.getBundling().getTimeWindowSeconds();
    }

    private double dropoffDistance(PendingOrder left, PendingOrder right) {
        return GeoUtil.haversineMeters(
                left.getCustomerLat(), left.getCustomerLon(),
                right.getCustomerLat(), right.getCustomerLon());
    }

    private List<Set<Long>> buildMergedPartnerPools(List<PendingOrder> candidates) {
        Map<Long, PartnerClusterNode> byPartner = new HashMap<>();
        for (PendingOrder order : candidates) {
            if (order.getPartnerId() == null || order.getId() == null) {
                continue;
            }
            byPartner.computeIfAbsent(order.getPartnerId(), ignored -> new PartnerClusterNode(order.getPartnerId()))
                    .add(order);
        }

        List<PartnerClusterNode> nodes = new ArrayList<>(byPartner.values());
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                PartnerClusterNode a = nodes.get(i);
                PartnerClusterNode b = nodes.get(j);
                double distance = GeoUtil.haversineMeters(a.centroidLat(), a.centroidLon(), b.centroidLat(), b.centroidLon());
                if (distance <= dispatchProperties.getBundling().getMerchantRadiusMeters()) {
                    a.union(b);
                }
            }
        }

        Map<PartnerClusterNode, Set<Long>> grouped = new LinkedHashMap<>();
        for (PendingOrder order : candidates) {
            if (order.getPartnerId() == null || order.getId() == null) {
                continue;
            }
            PartnerClusterNode root = byPartner.get(order.getPartnerId()).find();
            grouped.computeIfAbsent(root, ignored -> new TreeSet<>()).add(order.getId());
        }

        return new ArrayList<>(grouped.values());
    }

    private static final class PartnerClusterNode {
        private final Long partnerId;
        private double latSum = 0.0;
        private double lonSum = 0.0;
        private int count = 0;
        private PartnerClusterNode parent = this;

        private PartnerClusterNode(Long partnerId) {
            this.partnerId = partnerId;
        }

        private void add(PendingOrder order) {
            latSum += order.getPartnerLat();
            lonSum += order.getPartnerLon();
            count++;
        }

        private double centroidLat() {
            return count == 0 ? 0.0 : latSum / count;
        }

        private double centroidLon() {
            return count == 0 ? 0.0 : lonSum / count;
        }

        private PartnerClusterNode find() {
            if (parent != this) {
                parent = parent.find();
            }
            return parent;
        }

        private void union(PartnerClusterNode other) {
            PartnerClusterNode rootA = find();
            PartnerClusterNode rootB = other.find();
            if (rootA == rootB) {
                return;
            }
            rootB.parent = rootA;
            rootA.latSum += rootB.latSum;
            rootA.lonSum += rootB.lonSum;
            rootA.count += rootB.count;
        }
    }
}
