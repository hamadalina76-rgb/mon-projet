package com.speedline.delivery.matching.cost.service.impl;

import com.speedline.delivery.matching.cost.component.AvailabilityCostComponent;
import com.speedline.delivery.matching.cost.component.CostComponent;
import com.speedline.delivery.matching.cost.component.CourierTypeCostComponent;
import com.speedline.delivery.matching.cost.component.EtaTotalEstimatedCostComponent;
import com.speedline.delivery.matching.cost.component.GuaranteedDeadlineCostComponent;
import com.speedline.delivery.matching.cost.component.MerchantKnowledgeCostComponent;
import com.speedline.delivery.matching.cost.component.PerformanceRatingCostComponent;
import com.speedline.delivery.matching.cost.component.RecentRefusalCostComponent;
import com.speedline.delivery.matching.cost.component.RouteAlignmentCostComponent;
import com.speedline.delivery.matching.cost.component.TourCompatibilityCostComponent;
import com.speedline.delivery.matching.cost.component.VehicleCompatibilityCostComponent;
import com.speedline.delivery.matching.cost.component.WorkloadFairnessCostComponent;
import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostDecision;
import com.speedline.delivery.matching.cost.model.CostResult;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.EliminationReason;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import com.speedline.delivery.matching.cost.service.DispatchConfigService;
import com.speedline.delivery.matching.cost.service.ETAEstimator;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Disp201CostFunctionCoverageTest {

    @Test
    void shouldEvaluateAll11ComponentsWhenEnabled() {
        MutableConfigService config = new MutableConfigService(defaultSnapshotAllEnabled());
        CostFunctionServiceImpl service = new CostFunctionServiceImpl(fixedEta(12, 8, 15), config, allComponents());

        CostResult result = service.calculate(baseContext());

        assertEquals(CostDecision.PASSED, result.getDecision());
        assertEquals(11, result.getContributions().size());

        Set<CostComponentKey> keys = result.getContributions().stream()
                .map(c -> c.getKey())
                .collect(Collectors.toSet());

        assertEquals(EnumSet.allOf(CostComponentKey.class), keys);
    }

    @Test
    void shouldEnforceMandatoryComponentEvenWhenDisabledInConfig() {
        MutableConfigService config = new MutableConfigService(snapshot(
                cfg(CostComponentKey.GUARANTEED_DEADLINE, false, 10, 1, true)
        ));

        CostFunctionServiceImpl service = new CostFunctionServiceImpl(fixedEta(30, 10, 20), config, allComponents());

        ScoringContext context = ScoringContext.builder()
                .deliveryId(10L)
                .orderId(200L)
                .courierId(300L)
                .guaranteedDelayMinutes(40)
                .build();

        CostResult result = service.calculate(context);

        assertEquals(CostDecision.ELIMINATED, result.getDecision());
        assertEquals(EliminationReason.GUARANTEED_DELAY_EXCEEDED, result.getEliminationReason());
    }

    @Test
    void shouldRemoveComponentEffectWhenToggledOff() {
        MutableConfigService config = new MutableConfigService(defaultSnapshotAllEnabled());
        CostFunctionServiceImpl service = new CostFunctionServiceImpl(fixedEta(10, 8, 12), config, allComponents());

        CostResult allEnabled = service.calculate(baseContext());

        List<DispatchComponentConfig> adjusted = new ArrayList<>(defaultSnapshotAllEnabled().getComponents());
        adjusted = adjusted.stream()
                .map(c -> c.getKey() == CostComponentKey.MERCHANT_KNOWLEDGE
                        ? DispatchComponentConfig.builder()
                        .key(c.getKey())
                        .enabled(false)
                        .weight(c.getWeight())
                        .order(c.getOrder())
                        .mandatory(c.isMandatory())
                        .build()
                        : c)
                .toList();

        config.setSnapshot(DispatchConfigSnapshot.builder().components(adjusted).build());

        CostResult toggledOff = service.calculate(baseContext());

        assertEquals(10, toggledOff.getContributions().size());
        assertFalse(toggledOff.getContributions().stream().anyMatch(c -> c.getKey() == CostComponentKey.MERCHANT_KNOWLEDGE));
        assertTrue(allEnabled.getTotalCost() != toggledOff.getTotalCost());
    }

    @Test
    void shouldReflectWeightChangeImmediately() {
        MutableConfigService config = new MutableConfigService(snapshot(
                cfg(CostComponentKey.ROUTE_ALIGNMENT, true, 0, 1, false)
        ));

        CostFunctionServiceImpl service = new CostFunctionServiceImpl(fixedEta(10, 10, 10), config, allComponents());

        CostResult weight0 = service.calculate(baseContext());

        config.setSnapshot(snapshot(
                cfg(CostComponentKey.ROUTE_ALIGNMENT, true, 10, 1, false)
        ));

        CostResult weight10 = service.calculate(baseContext());

        assertTrue(weight10.getTotalCost() < weight0.getTotalCost());
    }

    @Test
    void shouldEliminateBulkyOrderWithIncompatibleVehicle() {
        MutableConfigService config = new MutableConfigService(defaultSnapshotAllEnabled());
        CostFunctionServiceImpl service = new CostFunctionServiceImpl(fixedEta(8, 6, 12), config, allComponents());

        ScoringContext incompatible = ScoringContext.builder()
                .deliveryId(11L)
                .orderId(201L)
                .courierId(301L)
                .vehicleType("BIKE")
                .bulkyOrder(true)
                .guaranteedDelayMinutes(60)
                .build();

        CostResult result = service.calculate(incompatible);

        assertEquals(CostDecision.ELIMINATED, result.getDecision());
        assertEquals(EliminationReason.VEHICLE_INCOMPATIBLE, result.getEliminationReason());
        assertEquals(infiniteCost(), result.getTotalCost());
    }

    @Test
    void performance500CalculationsShouldStayUnder100ms() {
        MutableConfigService config = new MutableConfigService(defaultSnapshotAllEnabled());
        CostFunctionServiceImpl service = new CostFunctionServiceImpl(fixedEta(10, 8, 15), config, allComponents());
        ScoringContext context = baseContext();

        // Warm up the JVM/JIT to reduce first-run noise in CI/IDE runs.
        for (int i = 0; i < 2_000; i++) {
            service.calculate(context);
        }

        long bestElapsedMs = Long.MAX_VALUE;
        for (int run = 0; run < 5; run++) {
            long start = System.nanoTime();
            for (int i = 0; i < 500; i++) {
                service.calculate(context);
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            bestElapsedMs = Math.min(bestElapsedMs, elapsedMs);
        }

        assertTrue(bestElapsedMs < 100, "Expected < 100ms for 500 calculations, best run got " + bestElapsedMs + "ms");
    }

    private ETAEstimator fixedEta(int courierToPickup, int prep, int pickupToDropoff) {
        return ctx -> ETAEstimation.builder()
                .courierToPickupMinutes(courierToPickup)
                .preparationMinutes(prep)
                .pickupToDropoffMinutes(pickupToDropoff)
                .build();
    }

    private List<CostComponent> allComponents() {
        return List.of(
                new EtaTotalEstimatedCostComponent(),
                new CourierTypeCostComponent(),
                new AvailabilityCostComponent(),
                new RouteAlignmentCostComponent(),
                new TourCompatibilityCostComponent(),
                new PerformanceRatingCostComponent(),
                new GuaranteedDeadlineCostComponent(),
                new RecentRefusalCostComponent(),
                new WorkloadFairnessCostComponent(),
                new MerchantKnowledgeCostComponent(),
                new VehicleCompatibilityCostComponent()
        );
    }

    private DispatchConfigSnapshot defaultSnapshotAllEnabled() {
        return snapshot(
                cfg(CostComponentKey.ETA_TOTAL_ESTIMATED, true, 10, 1, false),
                cfg(CostComponentKey.COURIER_TYPE, true, 10, 2, false),
                cfg(CostComponentKey.AVAILABILITY, true, 10, 3, false),
                cfg(CostComponentKey.ROUTE_ALIGNMENT, true, 8, 4, false),
                cfg(CostComponentKey.TOUR_COMPATIBILITY, true, 8, 5, false),
                cfg(CostComponentKey.PERFORMANCE_RATING, true, 7, 6, false),
                cfg(CostComponentKey.GUARANTEED_DEADLINE, true, 10, 7, true),
                cfg(CostComponentKey.RECENT_REFUSAL, true, 6, 8, false),
                cfg(CostComponentKey.WORKLOAD_FAIRNESS, true, 5, 9, false),
                cfg(CostComponentKey.MERCHANT_KNOWLEDGE, true, 4, 10, false),
                cfg(CostComponentKey.VEHICLE_COMPATIBILITY, true, 10, 11, true)
        );
    }

    private ScoringContext baseContext() {
        LocalDateTime now = LocalDateTime.now();
        return ScoringContext.builder()
                .deliveryId(1L)
                .orderId(100L)
                .courierId(10L)
                .courierLatitude(BigDecimal.valueOf(-0.002))
                .courierLongitude(BigDecimal.valueOf(-0.002))
                .pickupLatitude(BigDecimal.ZERO)
                .pickupLongitude(BigDecimal.ZERO)
                .dropoffLatitude(BigDecimal.valueOf(0.010))
                .dropoffLongitude(BigDecimal.valueOf(0.010))
                .currentRouteDropoffLatitude(BigDecimal.valueOf(0.008))
                .currentRouteDropoffLongitude(BigDecimal.valueOf(0.008))
                .courierType("INTERNAL")
                .vehicleType("MOTOR_TRICYCLE")
                .courierAvailable(true)
                .courierOnMission(true)
                .rating(BigDecimal.valueOf(4.6))
                .activeDeliveries(1)
                .guaranteedDelayMinutes(60)
                .bulkyOrder(false)
                .preparationMinutes(8)
                .lastRefusalAt(now.minusHours(2))
                .lastCompletedDeliveryAt(now.minusHours(3))
                .lastMerchantDeliveryAt(now.minusDays(1))
                .build();
    }

    private DispatchComponentConfig cfg(CostComponentKey key, boolean enabled, int weight, int order, boolean mandatory) {
        return DispatchComponentConfig.builder()
                .key(key)
                .enabled(enabled)
                .weight(weight)
                .order(order)
                .mandatory(mandatory)
                .build();
    }

    private DispatchConfigSnapshot snapshot(DispatchComponentConfig... configs) {
        List<DispatchComponentConfig> list = new ArrayList<>();
        for (DispatchComponentConfig config : configs) {
            list.add(config);
        }
        return DispatchConfigSnapshot.builder().components(list).build();
    }

    private double infiniteCost() {
        return new DispatchProperties().getMatching().getInfiniteCost();
    }

    private static class MutableConfigService implements DispatchConfigService {
        private DispatchConfigSnapshot snapshot;

        private MutableConfigService(DispatchConfigSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public DispatchConfigSnapshot getCurrentConfig() {
            return snapshot;
        }

        public void setSnapshot(DispatchConfigSnapshot snapshot) {
            this.snapshot = snapshot;
        }
    }
}
