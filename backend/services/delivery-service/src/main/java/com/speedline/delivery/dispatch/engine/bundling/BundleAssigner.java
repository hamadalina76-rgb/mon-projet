package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BundleAssigner {

    private final DispatchProperties dispatchProperties;

    public Optional<AvailableCourier> pickCourier(List<PendingOrder> bundle, List<AvailableCourier> internalCouriers) {
        if (bundle == null || bundle.isEmpty() || internalCouriers == null || internalCouriers.isEmpty()) {
            return Optional.empty();
        }

        PendingOrder first = bundle.get(0);
        if (first.getPartnerLat() == null || first.getPartnerLon() == null) {
            return Optional.empty();
        }

        String strategy = dispatchProperties.getBundling().getAssignmentStrategy();
        if (strategy == null || strategy.isBlank() || "nearest".equalsIgnoreCase(strategy)
                || "shortlist-or-tools".equalsIgnoreCase(strategy)) {
            return internalCouriers.stream()
                    .filter(c -> c.getId() != null && c.getLat() != null && c.getLon() != null)
                    .min(Comparator.comparingDouble(c -> GeoUtil.haversineMeters(
                            c.getLat(), c.getLon(), first.getPartnerLat(), first.getPartnerLon())));
        }

        return internalCouriers.stream().findFirst();
    }
}
