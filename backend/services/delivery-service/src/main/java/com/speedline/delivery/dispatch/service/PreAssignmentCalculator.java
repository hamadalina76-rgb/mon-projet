package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreAssignmentCalculator {

    private static final String ETA_FINISH_KEY = "courier:%d:currentDelivery:etaFinish";

    private final StringRedisTemplate redisTemplate;
    private final DispatchProperties properties;

    public List<AvailableCourier> enrichPreAssignable(List<AvailableCourier> base, Long zoneId, Clock clock) {
        if (base == null || base.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now(clock);
        int windowSeconds = properties.getPreAssignment().getFinishWindowSeconds();

        List<AvailableCourier> out = new ArrayList<>();
        Set<Long> present = new HashSet<>();

        for (AvailableCourier courier : base) {
            if (courier == null || courier.getId() == null) continue;

            if (courier.getStatus() == CourierStatus.IDLE || courier.getStatus() == CourierStatus.PRE_ASSIGNABLE) {
                out.add(courier);
                present.add(courier.getId());
                continue;
            }

            if (courier.getType() != CourierType.INTERNAL || courier.getStatus() != CourierStatus.ON_DELIVERY) {
                continue;
            }

            Instant etaFinish = readEtaFinish(courier.getId());
            if (etaFinish == null) {
                continue;
            }

            long seconds = Duration.between(now, etaFinish).toSeconds();
            if (seconds < 0 || seconds > windowSeconds) {
                continue;
            }

            if (present.add(courier.getId())) {
                out.add(AvailableCourier.builder()
                        .id(courier.getId())
                        .zoneId(courier.getZoneId())
                        .type(courier.getType())
                        .lat(courier.getLat())
                        .lon(courier.getLon())
                        .status(CourierStatus.PRE_ASSIGNABLE)
                        .vehicleType(courier.getVehicleType())
                        .rating(courier.getRating())
                        .currentLoad(courier.getCurrentLoad())
                        .maxCapacity(courier.getMaxCapacity())
                        .shiftStart(courier.getShiftStart())
                        .shiftEnd(courier.getShiftEnd())
                        .availableFromInstant(etaFinish)
                        .build());
            }
        }

        return out;
    }

    private Instant readEtaFinish(Long courierId) {
        try {
            String raw = redisTemplate.opsForValue().get(String.format(ETA_FINISH_KEY, courierId));
            if (raw == null || raw.isBlank()) return null;
            return Instant.parse(raw);
        } catch (Exception ex) {
            log.debug("Invalid etaFinish for courier {}: {}", courierId, ex.getMessage());
            return null;
        }
    }
}
