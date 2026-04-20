package com.speedline.delivery.dispatch.event;

import com.speedline.delivery.client.LocationServiceClient;
import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.client.PartnerServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderEnricher {

    private final PartnerServiceClient partnerServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final LocationServiceClient locationServiceClient;
    private final DispatchProperties dispatchProperties;

    public PendingOrder enrich(Map<String, Object> event) {
        return enrichOptional(event).orElse(null);
    }

    public Optional<PendingOrder> enrichOptional(Map<String, Object> event) {
        if (event == null || event.isEmpty()) {
            return Optional.empty();
        }

        final Long orderId = toLong(event.get("orderId")).orElse(null);
        final Long partnerId = toLong(event.get("partnerId")).orElse(null);
        final Long customerId = toLong(event.get("customerId")).orElse(null);
        if (orderId == null || partnerId == null || customerId == null) {
            return Optional.empty();
        }

        BigDecimal partnerLat = firstDecimal(event, "partnerLat", "partnerLatitude").orElse(null);
        BigDecimal partnerLon = firstDecimal(event, "partnerLon", "partnerLng", "partnerLongitude").orElse(null);
        BigDecimal customerLat = firstDecimal(event, "customerLat", "deliveryLatitude", "latitude").orElse(null);
        BigDecimal customerLon = firstDecimal(event, "customerLon", "deliveryLongitude", "longitude").orElse(null);
        Long zoneId = toLong(event.get("zoneId")).orElse(null);

        if (partnerLat == null || partnerLon == null) {
            try {
                Map<String, Object> partner = partnerServiceClient.getPartnerById(partnerId);
                partnerLat = Optional.ofNullable(partnerLat)
                        .or(() -> toDecimal(partner.get("latitude")))
                        .orElse(null);
                partnerLon = Optional.ofNullable(partnerLon)
                        .or(() -> toDecimal(partner.get("longitude")))
                        .orElse(null);
            } catch (Exception ex) {
                log.debug("Partner lookup failed partnerId={}: {}", partnerId, ex.getMessage());
            }
        }

        if (customerLat == null || customerLon == null) {
            try {
                Map<String, Object> order = orderServiceClient.getOrderById(orderId);
                Object deliveryAddress = order.get("deliveryAddress");
                if (deliveryAddress instanceof Map<?, ?> deliveryMap) {
                    customerLat = Optional.ofNullable(customerLat)
                            .or(() -> toDecimal(deliveryMap.get("latitude")))
                            .orElse(null);
                    customerLon = Optional.ofNullable(customerLon)
                            .or(() -> toDecimal(deliveryMap.get("longitude")))
                            .orElse(null);
                }
            } catch (Exception ex) {
                log.debug("Order lookup failed orderId={}: {}", orderId, ex.getMessage());
            }
        }

        final BigDecimal resolvedCustomerLat = customerLat;
        final BigDecimal resolvedCustomerLon = customerLon;
        final BigDecimal resolvedPartnerLat = partnerLat;
        final BigDecimal resolvedPartnerLon = partnerLon;

        zoneId = Optional.ofNullable(zoneId)
            .or(() -> resolveZoneId(resolvedCustomerLat, resolvedCustomerLon))
            .or(() -> resolveZoneId(resolvedPartnerLat, resolvedPartnerLon))
                .orElse(null);

        if (zoneId != null && (partnerLat == null || partnerLon == null || customerLat == null || customerLon == null)) {
            try {
                Map<String, Object> zone = locationServiceClient.getZoneById(zoneId);
                Object center = zone.get("center");
                if (center instanceof List<?> centerList && centerList.size() >= 2) {
                    BigDecimal centerLat = toDecimal(centerList.get(0)).orElse(null);
                    BigDecimal centerLon = toDecimal(centerList.get(1)).orElse(null);
                    partnerLat = firstNonNull(partnerLat, centerLat);
                    partnerLon = firstNonNull(partnerLon, centerLon);
                    customerLat = firstNonNull(customerLat, centerLat);
                    customerLon = firstNonNull(customerLon, centerLon);
                }
            } catch (Exception ex) {
                log.debug("Zone fallback lookup failed zoneId={}: {}", zoneId, ex.getMessage());
            }
        }

        if (zoneId == null) {
            zoneId = dispatchProperties.getEnricher().getDefaultZoneId();
        }

        return Optional.of(PendingOrder.builder()
                .id(orderId)
                .partnerId(partnerId)
                .customerId(customerId)
                .partnerLat(asDouble(partnerLat))
                .partnerLon(asDouble(partnerLon))
                .customerLat(asDouble(customerLat))
                .customerLon(asDouble(customerLon))
                .zoneId(zoneId)
                .guaranteedDeliveryMinutes(asInteger(
                    event.get("guaranteedDeliveryMinutes"),
                    dispatchProperties.getEnricher().getDefaultGuaranteedDeliveryMinutes()))
                .isUrgent(asBoolean(event.get("isUrgent"), false))
                .isLargeOrder(asBoolean(event.get("isLargeOrder"), false))
                .isScheduled(asBoolean(event.get("isScheduled"), false))
                .createdAt(asInstant(event.get("createdAt"), Instant.now()))
                .build());
    }

    private Optional<Long> resolveZoneId(BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) {
            return Optional.empty();
        }
        try {
            Map<String, Object> zone = locationServiceClient.findZoneForPoint(lat, lon);
            return toLong(zone == null ? null : zone.get("id"));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static Optional<BigDecimal> firstDecimal(Map<String, Object> map, String... keys) {
        return Stream.of(keys)
                .map(map::get)
                .map(PendingOrderEnricher::toDecimal)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<BigDecimal> toDecimal(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof BigDecimal b) {
            return Optional.of(b);
        }
        if (value instanceof Number n) {
            return Optional.of(BigDecimal.valueOf(n.doubleValue()));
        }
        try {
            return Optional.of(new BigDecimal(String.valueOf(value)));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static Optional<Long> toLong(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Number n) {
            return Optional.of(n.longValue());
        }
        try {
            return Optional.of(Long.parseLong(String.valueOf(value)));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static Integer asInteger(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static Instant asInstant(Object value, Instant fallback) {
        if (value == null) {
            return fallback;
        }
        return Stream.of(
                        value instanceof Instant instant ? instant : null,
                        value instanceof LocalDateTime localDateTime ? localDateTime.toInstant(ZoneOffset.UTC) : null,
                        parseInstantSafe(value))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(fallback);
    }

    private static Instant parseInstantSafe(Object value) {
        try {
            return Instant.parse(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private static Double asDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static BigDecimal firstNonNull(BigDecimal current, BigDecimal fallback) {
        return current != null ? current : fallback;
    }
}
