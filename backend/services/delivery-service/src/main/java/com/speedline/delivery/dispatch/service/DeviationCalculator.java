package com.speedline.delivery.dispatch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviationCalculator {

    static final String DEST_KEY = "courier:%d:currentDelivery:destination";

    private final StringRedisTemplate redisTemplate;
    private final DispatchProperties properties;
    private final ObjectMapper objectMapper;

    public boolean isDeviationEligible(AvailableCourier courier, double pickupLat, double pickupLon) {
        if (courier == null || courier.getId() == null || courier.getLat() == null || courier.getLon() == null) {
            return false;
        }
        String raw = redisTemplate.opsForValue().get(DEST_KEY.formatted(courier.getId()));
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            Map<String, Double> dest = objectMapper.readValue(raw, new TypeReference<>() {});
            Double destLat = dest.get("lat");
            Double destLon = dest.get("lon");
            if (destLat == null || destLon == null) {
                return false;
            }
            double extraMeters = GeoUtil.detourMeters(
                    courier.getLat(), courier.getLon(),
                    pickupLat, pickupLon,
                    destLat, destLon
            );
            double deviationMin = GeoUtil.toMinutes(extraMeters, properties.getUrgent().getCourierSpeedMps());
            return deviationMin <= properties.getUrgent().getMaxDeviationMinutes();
        } catch (Exception ex) {
            log.debug("DeviationCalculator failed courierId={}: {}", courier.getId(), ex.getMessage());
            return false;
        }
    }
}
