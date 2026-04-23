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

        // Fallback hydration may receive order-service payloads using "id" instead of "orderId".
        final Long orderId = toLong(event.get("orderId"))
                .or(() -> toLong(event.get("id")))
                .orElse(null);
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

        EnrichmentExtras extras = EnrichmentExtras.fromEvent(event);
        mergeOrderAndPartner(extras, orderId, partnerId);

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
                .orderNumber(extras.orderNumber)
                .customerName(extras.customerName)
                .customerPhone(extras.customerPhone)
                .partnerName(extras.partnerName)
                .pickupAddress(extras.pickupAddress)
                .dropoffAddress(extras.dropoffAddress)
                .deliveryInstructions(extras.deliveryInstructions)
                .deliveryFee(extras.deliveryFee)
                .isUrgent(asBoolean(event.get("isUrgent"), false))
                .isLargeOrder(asBoolean(event.get("isLargeOrder"), false))
                .isScheduled(asBoolean(event.get("isScheduled"), false))
                .scheduledDeliveryAt(asInstant(event.get("scheduledDeliveryAt"), null))
                .createdAt(asInstant(event.get("createdAt"), Instant.now()))
                .build());
    }

    private void mergeOrderAndPartner(EnrichmentExtras e, Long orderId, Long partnerId) {
        if (e.needsOrderDetails()) {
            try {
                Map<String, Object> order = orderServiceClient.getOrderById(orderId);
                e.mergeFromOrder(order);
            } catch (Exception ex) {
                log.debug("Order enrich failed orderId={}: {}", orderId, ex.getMessage());
            }
        }
        if (e.partnerName == null || e.pickupAddress == null) {
            try {
                Map<String, Object> partner = partnerServiceClient.getPartnerById(partnerId);
                if (e.partnerName == null) {
                    e.partnerName = firstString(partner, "name", "displayName", "businessName");
                }
                if (e.pickupAddress == null) {
                    e.pickupAddress = firstString(partner, "address", "fullAddress", "pickupAddress");
                }
            } catch (Exception ex) {
                log.debug("Partner enrich failed partnerId={}: {}", partnerId, ex.getMessage());
            }
        }
    }

    private static final class EnrichmentExtras {
        String orderNumber;
        String customerName;
        String customerPhone;
        String partnerName;
        String pickupAddress;
        String dropoffAddress;
        String deliveryInstructions;
        BigDecimal deliveryFee;

        static EnrichmentExtras fromEvent(Map<String, Object> event) {
            EnrichmentExtras e = new EnrichmentExtras();
            e.orderNumber = firstString(
                    event, "orderNumber", "orderCode", "reference", "orderReference");
            e.customerName = firstString(
                    event, "customerName", "customerDisplayName", "clientName");
            e.customerPhone = firstString(
                    event, "customerPhone", "clientPhone", "phone", "phoneNumber");
            e.partnerName = firstString(
                    event, "partnerName", "merchantName", "restaurantName");
            e.pickupAddress = firstString(
                    event, "pickupAddress", "partnerAddress", "restaurantAddress");
            e.dropoffAddress = firstString(
                    event, "dropoffAddress", "deliveryAddress", "address");
            e.deliveryInstructions = firstString(
                    event, "deliveryInstructions", "specialInstructions", "note");
            e.deliveryFee = firstDecimal(event, "deliveryFee", "shippingFee", "totalDeliveryFee")
                    .orElse(null);
            return e;
        }

        void mergeFromOrder(Map<String, Object> order) {
            if (orderNumber == null) {
                orderNumber = firstString(
                        order, "orderNumber", "number", "reference", "displayId");
            }
            if (deliveryFee == null) {
                deliveryFee = firstDecimal(
                        order, "deliveryFee", "shippingAmount", "deliveryCharge").orElse(null);
            }
            if (deliveryInstructions == null) {
                deliveryInstructions = firstString(
                        order, "deliveryInstructions", "note", "specialNotes");
            }
            Object u = order.get("customer");
            if (u instanceof Map<?, ?> m) {
                if (customerName == null) {
                    customerName = firstString(
                            m, "name", "firstName", "fullName", "displayName");
                }
                if (customerPhone == null) {
                    customerPhone = firstString(m, "phone", "phoneNumber", "mobile");
                }
            }
            if (dropoffAddress == null) {
                Object d = order.get("deliveryAddress");
                if (d instanceof Map<?, ?> m) {
                    String line = firstString(
                            m, "formattedAddress", "fullAddress", "street", "line1", "line2");
                    if (line != null) {
                        dropoffAddress = line;
                    } else {
                        StringBuilder b = new StringBuilder();
                        appendIf(b, m.get("line1"));
                        appendIf(b, m.get("line2"));
                        appendIf(b, m.get("city"));
                        if (!b.isEmpty()) {
                            dropoffAddress = b.toString().trim();
                        }
                    }
                }
            }
        }

        boolean needsOrderDetails() {
            return firstBlank(orderNumber)
                    || firstBlank(customerName)
                    || firstBlank(customerPhone)
                    || firstBlank(dropoffAddress)
                    || deliveryFee == null;
        }

        private static void appendIf(StringBuilder b, Object part) {
            if (part == null) {
                return;
            }
            String s = String.valueOf(part).trim();
            if (s.isEmpty()) {
                return;
            }
            if (!b.isEmpty()) {
                b.append(", ");
            }
            b.append(s);
        }

        private static boolean firstBlank(String s) {
            return s == null || s.isBlank();
        }
    }

    private static String firstString(Map<?, ?> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private static Optional<BigDecimal> firstDecimal(Map<?, ?> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            Optional<BigDecimal> d = toDecimal(v);
            if (d.isPresent()) {
                return d;
            }
        }
        return Optional.empty();
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
