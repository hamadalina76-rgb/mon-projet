package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearestNeighborRouterTest {

    @Test
    void deterministicRoute_monotonicEtas() {
        DispatchProperties props = new DispatchProperties();
        props.getBundling().setAverageSpeedKmh(25.0);
        NearestNeighborRouter router = new NearestNeighborRouter(props);

        AvailableCourier courier = AvailableCourier.builder().id(1L).lat(36.8).lon(10.1).build();
        List<PendingOrder> bundle = List.of(
                order(1L, 36.8000, 10.1000, 36.8100, 10.1100),
                order(2L, 36.8002, 10.1001, 36.8110, 10.1110),
                order(3L, 36.8004, 10.1002, 36.8120, 10.1120)
        );

        BundleRoute route = router.route(courier, bundle);
        assertEquals(3, route.etaDeliveryMinByOrder().size());

        int e1 = route.etaDeliveryMinByOrder().get(1L);
        int e2 = route.etaDeliveryMinByOrder().get(2L);
        int e3 = route.etaDeliveryMinByOrder().get(3L);
        assertTrue(e1 > 0);
        assertTrue(e2 > 0);
        assertTrue(e3 > 0);
    }

    private static PendingOrder order(Long id, double pl, double po, double cl, double co) {
        return PendingOrder.builder()
                .id(id)
                .partnerId(100L + id)
                .partnerLat(pl)
                .partnerLon(po)
                .customerLat(cl)
                .customerLon(co)
                .createdAt(Instant.now())
                .build();
    }
}
