package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class NearestNeighborRouter implements BundleRouter {

    private final DispatchProperties properties;

    @Override
    public BundleRoute route(AvailableCourier courier, List<PendingOrder> bundleOrders) {
        if (bundleOrders == null || bundleOrders.isEmpty()) {
            return BundleRoute.builder()
                    .stops(List.of())
                    .etaPickupMinByOrder(Map.of())
                    .etaDeliveryMinByOrder(Map.of())
                    .build();
        }

        double speed = properties.getBundling().getAverageSpeedKmh();
        double currentLat = courier != null && courier.getLat() != null ? courier.getLat() : bundleOrders.get(0).getPartnerLat();
        double currentLon = courier != null && courier.getLon() != null ? courier.getLon() : bundleOrders.get(0).getPartnerLon();
        int elapsed = 0;

        List<BundleRoute.Stop> stops = new ArrayList<>();
        Map<Long, Integer> pickupEta = new HashMap<>();
        Map<Long, Integer> deliveryEta = new HashMap<>();

        Map<Long, List<PendingOrder>> byPartner = new LinkedHashMap<>();
        for (PendingOrder order : bundleOrders) {
            byPartner.computeIfAbsent(order.getPartnerId(), ignored -> new ArrayList<>()).add(order);
        }

        List<Map.Entry<Long, List<PendingOrder>>> remainingPartners = new ArrayList<>(byPartner.entrySet());
        while (!remainingPartners.isEmpty()) {
            final double refLat = currentLat;
            final double refLon = currentLon;
            Map.Entry<Long, List<PendingOrder>> next = remainingPartners.stream()
                    .filter(e -> !e.getValue().isEmpty() && e.getValue().get(0).getPartnerLat() != null && e.getValue().get(0).getPartnerLon() != null)
                    .min(Comparator.comparingDouble(e -> GeoUtil.haversineMeters(
                            refLat,
                            refLon,
                            e.getValue().get(0).getPartnerLat(),
                            e.getValue().get(0).getPartnerLon())))
                    .orElse(remainingPartners.get(0));

            PendingOrder ref = next.getValue().get(0);
            elapsed += GeoUtil.travelMinutes(GeoUtil.haversineMeters(currentLat, currentLon, ref.getPartnerLat(), ref.getPartnerLon()), speed);
            currentLat = ref.getPartnerLat();
            currentLon = ref.getPartnerLon();

            for (PendingOrder order : next.getValue()) {
                pickupEta.put(order.getId(), elapsed);
                stops.add(BundleRoute.Stop.builder()
                        .orderId(order.getId())
                        .type("PICKUP")
                        .lat(order.getPartnerLat())
                        .lon(order.getPartnerLon())
                        .etaMin(elapsed)
                        .build());
            }
            remainingPartners.remove(next);
        }

        List<PendingOrder> remainingDropoffs = new ArrayList<>(bundleOrders);
        while (!remainingDropoffs.isEmpty()) {
            final double refLat = currentLat;
            final double refLon = currentLon;
            PendingOrder next = remainingDropoffs.stream()
                    .filter(o -> o.getCustomerLat() != null && o.getCustomerLon() != null)
                    .min(Comparator.comparingDouble(o -> GeoUtil.haversineMeters(refLat, refLon, o.getCustomerLat(), o.getCustomerLon())))
                    .orElse(remainingDropoffs.get(0));

            elapsed += GeoUtil.travelMinutes(GeoUtil.haversineMeters(currentLat, currentLon, next.getCustomerLat(), next.getCustomerLon()), speed);
            currentLat = next.getCustomerLat();
            currentLon = next.getCustomerLon();
            deliveryEta.put(next.getId(), elapsed);
            stops.add(BundleRoute.Stop.builder()
                    .orderId(next.getId())
                    .type("DROPOFF")
                    .lat(next.getCustomerLat())
                    .lon(next.getCustomerLon())
                    .etaMin(elapsed)
                    .build());
            remainingDropoffs.remove(next);
        }

        return BundleRoute.builder()
                .stops(stops)
                .etaPickupMinByOrder(pickupEta)
                .etaDeliveryMinByOrder(deliveryEta)
                .build();
    }
}
