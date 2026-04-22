package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.client.LocationServiceClient;
import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.config.DispatchZoneModeStore;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.dto.CourierPositionView;
import com.speedline.delivery.dispatch.dto.DispatchDashboardKpisResponse;
import com.speedline.delivery.dispatch.dto.DispatchZoneMetricsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchDashboardQueryService {

    private final DispatchDashboardKpiService kpiService;
    private final DispatchZoneConfig zoneConfig;
    private final CourierAvailabilityService courierAvailabilityService;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final OrderServiceClient orderServiceClient;
    private final LocationServiceClient locationServiceClient;
    private final DispatchZoneModeStore zoneModeStore;

    public DispatchDashboardKpisResponse getKpis() {
        return kpiService.getGlobalKpis();
    }

    public DispatchZoneMetricsResponse getZoneMetrics(Long zoneId) {
        List<AvailableCourier> online = courierAvailabilityService.findOnlineByZone(zoneId);
        long idle = online.stream().filter(c -> "IDLE".equalsIgnoreCase(String.valueOf(c.getStatus()))).count();
        long onDelivery = online.stream().filter(c -> "ON_DELIVERY".equalsIgnoreCase(String.valueOf(c.getStatus()))).count();
        long pending = pendingOrderRedisRepository.countByZone(zoneId);
        String zoneName = null;
        Boolean zoneActive = null;
        try {
            Map<String, Object> z = locationServiceClient.getZoneById(zoneId);
            if (z != null) {
                Object name = z.get("name");
                if (name != null) {
                    zoneName = String.valueOf(name);
                }
                zoneActive = asBoolean(z.get("isActive"));
            }
        } catch (Exception ex) {
            log.warn("Impossible de charger la zone {} depuis location-service: {}", zoneId, ex.getMessage());
        }
        return DispatchZoneMetricsResponse.builder()
                .zoneId(zoneId)
                .zoneName(zoneName)
                .zoneActive(zoneActive)
                .mode(zoneConfig.getMode(zoneId))
                .runtimeModeOverride(zoneModeStore.getOverride(zoneId).isPresent())
                .onlineCouriers(online.size())
                .idleCouriers(idle)
                .onDeliveryCouriers(onDelivery)
                .pendingOrders(pending)
                .averageAssignmentDelaySeconds(getKpis().getAverageAssignmentDelaySeconds())
                .build();
    }

    private static Boolean asBoolean(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    public List<CourierPositionView> getCourierPositions() {
        List<CourierPositionView> out = new ArrayList<>();
        zoneConfig.getZones().forEach(zone -> {
            Long zoneId = zone.getId();
            for (AvailableCourier c : courierAvailabilityService.findOnlineByZone(zoneId)) {
                out.add(CourierPositionView.builder()
                        .courierId(c.getId())
                        .zoneId(zoneId)
                        .type(c.getType())
                        .status(String.valueOf(c.getStatus()))
                        .lat(c.getLat())
                        .lon(c.getLon())
                        .online(true)
                        .build());
            }
        });
        return out.stream()
                .collect(Collectors.toMap(
                        CourierPositionView::getCourierId,
                        c -> c,
                        (a, b) -> a))
                .values()
                .stream()
                .sorted(Comparator.comparing(CourierPositionView::getCourierId))
                .toList();
    }

    public List<Map<String, Object>> getPendingOrders() {
        return orderServiceClient.getOrdersAwaitingCourier();
    }
}
