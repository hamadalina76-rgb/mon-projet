package com.speedline.delivery.dispatch.config.service;

import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.matching.cost.model.ScoringContext;

import java.math.BigDecimal;

public final class DispatchScoringContextMapper {

    private DispatchScoringContextMapper() {
    }

    public static ScoringContext from(PendingOrder order, AvailableCourier courier) {
        boolean available = courier.getStatus() == null || courier.getStatus() == CourierStatus.IDLE
                || courier.getStatus() == CourierStatus.PRE_ASSIGNABLE;
        boolean onMission = courier.getStatus() == CourierStatus.ON_DELIVERY;
        int load = courier.getCurrentLoad() == null ? 0 : courier.getCurrentLoad();
        return ScoringContext.builder()
                .orderId(order.getId())
                .courierId(courier.getId())
                .courierLatitude(toBd(courier.getLat()))
                .courierLongitude(toBd(courier.getLon()))
                .pickupLatitude(toBd(order.getPartnerLat()))
                .pickupLongitude(toBd(order.getPartnerLon()))
                .dropoffLatitude(toBd(order.getCustomerLat()))
                .dropoffLongitude(toBd(order.getCustomerLon()))
                .currentRouteDropoffLatitude(toBd(order.getCustomerLat()))
                .currentRouteDropoffLongitude(toBd(order.getCustomerLon()))
                .courierType(courier.getType() == CourierType.EXTERNAL ? "EXTERNAL" : "INTERNAL")
                .vehicleType(courier.getVehicleType() != null ? courier.getVehicleType() : "MOTORCYCLE")
                .courierAvailable(available)
                .courierOnMission(onMission)
                .rating(courier.getRating() != null ? BigDecimal.valueOf(courier.getRating()) : null)
                .activeDeliveries(load)
                .guaranteedDelayMinutes(order.getGuaranteedDeliveryMinutes())
                .bulkyOrder(Boolean.TRUE.equals(order.getIsLargeOrder()))
                .preparationMinutes(10)
                .build();
    }

    private static BigDecimal toBd(Double v) {
        return v == null ? null : BigDecimal.valueOf(v);
    }
}
