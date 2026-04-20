package com.speedline.delivery.dispatch.contract.engine;

import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solver input matrix containing order-courier costs.
 * Contract-Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostMatrix {
    @Builder.Default
    private List<PendingOrder> orders = new ArrayList<>();

    @Builder.Default
    private List<AvailableCourier> couriers = new ArrayList<>();

    /** orderId -> courierId -> costResult */
    @Builder.Default
    private Map<Long, Map<Long, CostResult>> entries = new HashMap<>();

    public void put(Long orderId, Long courierId, CostResult costResult) {
        entries.computeIfAbsent(orderId, ignored -> new HashMap<>()).put(courierId, costResult);
    }

    public CostResult get(Long orderId, Long courierId) {
        Map<Long, CostResult> byCourier = entries.get(orderId);
        if (byCourier == null) return null;
        return byCourier.get(courierId);
    }
}
