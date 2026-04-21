package com.speedline.delivery.matching.cost.service.impl;

import com.speedline.delivery.matching.cost.component.AvailabilityCostComponent;
import com.speedline.delivery.matching.cost.component.CostComponent;
import com.speedline.delivery.matching.cost.component.CourierTypeCostComponent;
import com.speedline.delivery.matching.cost.component.EtaTotalEstimatedCostComponent;
import com.speedline.delivery.matching.cost.component.GuaranteedDeadlineCostComponent;
import com.speedline.delivery.matching.cost.component.RouteAlignmentCostComponent;
import com.speedline.delivery.matching.cost.component.VehicleCompatibilityCostComponent;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CostFunctionServiceImplTest {

    @Test
    void ahmedShouldHaveLowerCostThanKarim() {
        ETAEstimator etaEstimator = ctx -> ETAEstimation.builder()
                .courierToPickupMinutes(8)
                .preparationMinutes(5)
                .pickupToDropoffMinutes(10)
                .build();

        MutableConfigService config = new MutableConfigService(snapshot(
                cfg(CostComponentKey.ETA_TOTAL_ESTIMATED, true, 10, 1, false),
                cfg(CostComponentKey.COURIER_TYPE, true, 10, 2, false),
                cfg(CostComponentKey.AVAILABILITY, true, 10, 3, false),
                cfg(CostComponentKey.ROUTE_ALIGNMENT, true, 8, 4, false)
        ));

        CostFunctionServiceImpl service = new CostFunctionServiceImpl(
                etaEstimator,
                config,
                List.of(
                        new EtaTotalEstimatedCostComponent(),
                        new CourierTypeCostComponent(),
                        new AvailabilityCostComponent(),
                        new RouteAlignmentCostComponent()
                )
        );

        ScoringContext ahmed = ScoringContext.builder()
                .deliveryId(1L)
                .orderId(100L)
                .courierId(10L)
                .courierLatitude(BigDecimal.valueOf(-0.003))
                .courierLongitude(BigDecimal.valueOf(-0.003))
                .pickupLatitude(BigDecimal.ZERO)
                .pickupLongitude(BigDecimal.ZERO)
                .dropoffLatitude(BigDecimal.valueOf(0.010))
                .dropoffLongitude(BigDecimal.valueOf(0.010))
                .courierType("INTERNAL")
                .courierAvailable(true)
                .courierOnMission(false)
                .build();

        ScoringContext karim = ScoringContext.builder()
                .deliveryId(1L)
                .orderId(100L)
                .courierId(11L)
                .courierLatitude(BigDecimal.valueOf(0.002))
                .courierLongitude(BigDecimal.valueOf(0.002))
                .pickupLatitude(BigDecimal.ZERO)
                .pickupLongitude(BigDecimal.ZERO)
                .dropoffLatitude(BigDecimal.valueOf(0.010))
                .dropoffLongitude(BigDecimal.valueOf(0.010))
                .courierType("EXTERNAL")
                .courierAvailable(true)
                .courierOnMission(false)
                .build();

        CostResult ahmedCost = service.calculate(ahmed);
        CostResult karimCost = service.calculate(karim);

        assertTrue(ahmedCost.getTotalCost() < karimCost.getTotalCost());
    }

    @Test
    void shouldEliminateWhenEtaExceedsGuaranteedDelay() {
        ETAEstimator etaEstimator = ctx -> ETAEstimation.builder()
                .courierToPickupMinutes(20)
                .preparationMinutes(10)
                .pickupToDropoffMinutes(25)
                .build();

        MutableConfigService config = new MutableConfigService(snapshot(
                cfg(CostComponentKey.GUARANTEED_DEADLINE, true, 10, 1, true)
        ));

        CostFunctionServiceImpl service = new CostFunctionServiceImpl(
                etaEstimator,
                config,
                List.of(new GuaranteedDeadlineCostComponent())
        );

        ScoringContext context = ScoringContext.builder()
                .deliveryId(2L)
                .orderId(101L)
                .courierId(20L)
                .guaranteedDelayMinutes(30)
                .build();

        CostResult result = service.calculate(context);

        assertEquals(CostDecision.ELIMINATED, result.getDecision());
        assertEquals(EliminationReason.GUARANTEED_DELAY_EXCEEDED, result.getEliminationReason());
        assertEquals(infiniteCost(), result.getTotalCost());
    }

    @Test
    void shouldEliminateBulkyOrderWithBike() {
        ETAEstimator etaEstimator = ctx -> ETAEstimation.builder()
                .courierToPickupMinutes(5)
                .preparationMinutes(5)
                .pickupToDropoffMinutes(5)
                .build();

        MutableConfigService config = new MutableConfigService(snapshot(
                cfg(CostComponentKey.VEHICLE_COMPATIBILITY, true, 10, 1, true)
        ));

        CostFunctionServiceImpl service = new CostFunctionServiceImpl(
                etaEstimator,
                config,
                List.of(new VehicleCompatibilityCostComponent())
        );

        ScoringContext context = ScoringContext.builder()
                .deliveryId(3L)
                .orderId(102L)
                .courierId(30L)
                .vehicleType("BIKE")
                .bulkyOrder(true)
                .build();

        CostResult result = service.calculate(context);

        assertEquals(CostDecision.ELIMINATED, result.getDecision());
        assertEquals(EliminationReason.VEHICLE_INCOMPATIBLE, result.getEliminationReason());
    }

    @Test
    void changingRouteAlignmentWeightShouldChangeCostImmediately() {
        ETAEstimator etaEstimator = ctx -> ETAEstimation.builder()
                .courierToPickupMinutes(10)
                .preparationMinutes(10)
                .pickupToDropoffMinutes(10)
                .build();

        MutableConfigService config = new MutableConfigService(snapshot(
                cfg(CostComponentKey.ROUTE_ALIGNMENT, true, 0, 1, false)
        ));

        CostFunctionServiceImpl service = new CostFunctionServiceImpl(
                etaEstimator,
                config,
                List.of(new RouteAlignmentCostComponent())
        );

        ScoringContext context = ScoringContext.builder()
                .deliveryId(4L)
                .orderId(103L)
                .courierId(40L)
                .courierLatitude(BigDecimal.valueOf(-0.001))
                .courierLongitude(BigDecimal.valueOf(-0.001))
                .pickupLatitude(BigDecimal.ZERO)
                .pickupLongitude(BigDecimal.ZERO)
                .dropoffLatitude(BigDecimal.valueOf(0.005))
                .dropoffLongitude(BigDecimal.valueOf(0.005))
                .build();

        CostResult weight0 = service.calculate(context);

        config.setSnapshot(snapshot(
                cfg(CostComponentKey.ROUTE_ALIGNMENT, true, 10, 1, false)
        ));

        CostResult weight10 = service.calculate(context);

        assertTrue(weight10.getTotalCost() < weight0.getTotalCost());
    }

    @Test
    void disabledComponentShouldNotContribute() {
        ETAEstimator etaEstimator = ctx -> ETAEstimation.builder()
                .courierToPickupMinutes(10)
                .preparationMinutes(10)
                .pickupToDropoffMinutes(10)
                .build();

        MutableConfigService config = new MutableConfigService(snapshot(
                cfg(CostComponentKey.COURIER_TYPE, false, 10, 1, false)
        ));

        CostFunctionServiceImpl service = new CostFunctionServiceImpl(
                etaEstimator,
                config,
                List.of(new CourierTypeCostComponent())
        );

        ScoringContext externalCourier = ScoringContext.builder()
                .deliveryId(5L)
                .orderId(104L)
                .courierId(50L)
                .courierType("EXTERNAL")
                .build();

        CostResult result = service.calculate(externalCourier);

        assertEquals(0.0, result.getTotalCost());
        assertEquals(0, result.getContributions().size());
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
