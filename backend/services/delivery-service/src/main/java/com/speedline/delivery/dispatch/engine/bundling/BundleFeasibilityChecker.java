package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.util.GeoUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class BundleFeasibilityChecker {

    public FeasibilityResult isFeasible(List<PendingOrder> members, double averageSpeedKmh) {
        if (members == null || members.isEmpty()) {
            return new FeasibilityResult(false, Map.of(), "empty");
        }

        PendingOrder first = members.get(0);
        if (first.getPartnerLat() == null || first.getPartnerLon() == null) {
            return new FeasibilityResult(false, Map.of(), "invalid_coordinates");
        }

        double currentLat = first.getPartnerLat();
        double currentLon = first.getPartnerLon();
        int elapsedMinutes = 0;

        Map<Long, List<PendingOrder>> byPartner = new LinkedHashMap<>();
        for (PendingOrder order : members) {
            if (order == null || order.getId() == null || order.getPartnerId() == null
                    || order.getPartnerLat() == null || order.getPartnerLon() == null
                    || order.getCustomerLat() == null || order.getCustomerLon() == null) {
                return new FeasibilityResult(false, Map.of(), "invalid_coordinates");
            }
            byPartner.computeIfAbsent(order.getPartnerId(), k -> new ArrayList<>()).add(order);
        }

        Set<Long> visitedPartner = new LinkedHashSet<>();
        for (PendingOrder member : members) {
            if (!visitedPartner.add(member.getPartnerId())) {
                continue;
            }
            elapsedMinutes += travel(currentLat, currentLon, member.getPartnerLat(), member.getPartnerLon(), averageSpeedKmh);
            currentLat = member.getPartnerLat();
            currentLon = member.getPartnerLon();
        }

        Map<Long, Integer> etaByOrder = new HashMap<>();
        for (PendingOrder member : members) {
            elapsedMinutes += travel(currentLat, currentLon, member.getCustomerLat(), member.getCustomerLon(), averageSpeedKmh);
            currentLat = member.getCustomerLat();
            currentLon = member.getCustomerLon();
            etaByOrder.put(member.getId(), elapsedMinutes);

            Integer guaranteed = member.getGuaranteedDeliveryMinutes();
            if (guaranteed != null && elapsedMinutes > guaranteed) {
                return new FeasibilityResult(false, Map.of(), "guarantee");
            }
        }

        return new FeasibilityResult(true, etaByOrder, "ok");
    }

    private int travel(double fromLat, double fromLon, double toLat, double toLon, double speedKmh) {
        return GeoUtil.travelMinutes(GeoUtil.haversineMeters(fromLat, fromLon, toLat, toLon), speedKmh);
    }

    public record FeasibilityResult(boolean feasible, Map<Long, Integer> perOrderEtaMin, String reason) {
    }
}
