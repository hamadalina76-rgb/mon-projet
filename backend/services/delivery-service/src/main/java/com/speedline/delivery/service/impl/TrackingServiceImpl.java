package com.speedline.delivery.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.domain.Delivery;
import com.speedline.delivery.domain.TrackingPoint;
import com.speedline.delivery.dto.DeliveryDTO;
import com.speedline.delivery.dto.TrackingPointDTO;
import com.speedline.delivery.dto.TrackingUpdateDTO;
import com.speedline.delivery.repository.DeliveryRepository;
import com.speedline.delivery.repository.TrackingPointRepository;
import com.speedline.delivery.service.DeliveryService;
import com.speedline.delivery.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implémentation du service de tracking GPS
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrackingServiceImpl implements TrackingService {

    private static final double DEFAULT_COURIER_SPEED_KMH = 15.0;
    private static final double EARTH_R_KM = 6371.0;

    private final DeliveryRepository deliveryRepository;
    private final TrackingPointRepository trackingPointRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final DeliveryService deliveryService;

    @Override
    @Transactional
    public void recordLocation(TrackingUpdateDTO update) {
        Delivery d = deliveryRepository.findById(update.getDeliveryId())
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + update.getDeliveryId()));
        if (d.getCourierId() == null) {
            throw new IllegalStateException("Aucun livreur assigne pour cette livraison");
        }

        TrackingPoint point = TrackingPoint.builder()
                .deliveryId(d.getId())
                .latitude(update.getLatitude())
                .longitude(update.getLongitude())
                .accuracy(update.getAccuracy())
                .altitude(update.getAltitude())
                .speed(update.getSpeed())
                .bearing(update.getBearing())
                .batteryLevel(update.getBatteryLevel())
                .deliveryStatus(d.getStatus())
                .build();
        trackingPointRepository.save(point);

        try {
            Map<String, Object> pos = new HashMap<>();
            pos.put("lat", update.getLatitude().doubleValue());
            pos.put("lng", update.getLongitude().doubleValue());
            if (update.getAccuracy() != null) {
                pos.put("accuracy", update.getAccuracy().doubleValue());
            }
            if (update.getSpeed() != null) {
                pos.put("speed", update.getSpeed().doubleValue());
            }
            if (update.getBearing() != null) {
                pos.put("heading", update.getBearing().doubleValue());
            }
            if (update.getBatteryLevel() != null) {
                pos.put("batteryLevel", update.getBatteryLevel());
            }
            pos.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).toString());
            String json = objectMapper.writeValueAsString(pos);
            String key = "courier:%s:position".formatted(d.getCourierId());
            stringRedisTemplate.opsForValue().set(key, json, java.time.Duration.ofMinutes(5));
        } catch (Exception ex) {
            log.warn("Failed to update Redis position for delivery {}: {}", d.getId(), ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingPointDTO getLastLocation(Long deliveryId) {
        deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));
        return trackingPointRepository.findFirstByDeliveryIdOrderByTimestampDesc(deliveryId)
                .map(this::toPointDto)
                .or(() -> fromRedis(deliveryId))
                .orElse(null);
    }

    private Optional<TrackingPointDTO> fromRedis(Long deliveryId) {
        Delivery d = deliveryRepository.findById(deliveryId).orElse(null);
        if (d == null || d.getCourierId() == null) {
            return Optional.empty();
        }
        String raw = stringRedisTemplate.opsForValue()
                .get("courier:%s:position".formatted(d.getCourierId()));
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> m = objectMapper.readValue(raw, new TypeReference<>() { });
            Object lat = m.get("lat");
            Object lng = m.get("lng");
            if (lat == null && m.get("latitude") != null) {
                lat = m.get("latitude");
            }
            if (lng == null && m.get("longitude") != null) {
                lng = m.get("longitude");
            }
            if (lat == null || lng == null) {
                return Optional.empty();
            }
            return Optional.of(TrackingPointDTO.builder()
                    .deliveryId(deliveryId)
                    .latitude(new BigDecimal(String.valueOf(lat)))
                    .longitude(new BigDecimal(String.valueOf(lng)))
                    .build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackingPointDTO> getTrackingHistory(Long deliveryId) {
        if (!deliveryRepository.existsById(deliveryId)) {
            throw new IllegalArgumentException("Livraison introuvable: " + deliveryId);
        }
        return trackingPointRepository.findByDeliveryIdOrderByTimestampAsc(deliveryId).stream()
                .map(this::toPointDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryDTO getRealtimeTracking(Long deliveryId) {
        DeliveryDTO base = deliveryService.getDeliveryById(deliveryId);
        fromRedis(deliveryId).ifPresent(p -> {
            base.setCurrentLatitude(p.getLatitude());
            base.setCurrentLongitude(p.getLongitude());
            base.setLastLocationUpdate(p.getTimestamp());
        });
        return base;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer estimateArrivalTime(Long deliveryId) {
        Delivery d = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));
        if (d.getDropoffLatitude() == null || d.getDropoffLongitude() == null) {
            return null;
        }
        Optional<TrackingPointDTO> last = Optional.ofNullable(getLastLocation(deliveryId));
        if (last.isEmpty() || last.get().getLatitude() == null) {
            return d.getEstimatedDuration();
        }
        double km = haversineKm(
                last.get().getLatitude().doubleValue(),
                last.get().getLongitude().doubleValue(),
                d.getDropoffLatitude().doubleValue(),
                d.getDropoffLongitude().doubleValue());
        int minutes = (int) Math.ceil((km / DEFAULT_COURIER_SPEED_KMH) * 60.0);
        return Math.max(1, minutes);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRemainingDistance(Long deliveryId) {
        Delivery d = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));
        if (d.getDropoffLatitude() == null) {
            return null;
        }
        Optional<TrackingPointDTO> last = Optional.ofNullable(getLastLocation(deliveryId));
        if (last.isEmpty() || last.get().getLatitude() == null) {
            return d.getEstimatedDistance();
        }
        double km = haversineKm(
                last.get().getLatitude().doubleValue(),
                last.get().getLongitude().doubleValue(),
                d.getDropoffLatitude().doubleValue(),
                d.getDropoffLongitude().doubleValue());
        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalDistance(Long deliveryId) {
        List<TrackingPoint> pts = trackingPointRepository.findByDeliveryIdOrderByTimestampAsc(deliveryId);
        if (pts.size() < 2) {
            return BigDecimal.ZERO;
        }
        double sum = 0;
        for (int i = 1; i < pts.size(); i++) {
            sum += haversineKm(
                    pts.get(i - 1).getLatitude().doubleValue(),
                    pts.get(i - 1).getLongitude().doubleValue(),
                    pts.get(i).getLatitude().doubleValue(),
                    pts.get(i).getLongitude().doubleValue());
        }
        return BigDecimal.valueOf(sum).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourierNearPickup(Long deliveryId, int thresholdMeters) {
        return isNear(deliveryId, true, thresholdMeters);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourierNearDropoff(Long deliveryId, int thresholdMeters) {
        return isNear(deliveryId, false, thresholdMeters);
    }

    private boolean isNear(Long deliveryId, boolean pickup, int thresholdMeters) {
        Delivery d = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));
        BigDecimal tlat = pickup ? d.getPickupLatitude() : d.getDropoffLatitude();
        BigDecimal tlon = pickup ? d.getPickupLongitude() : d.getDropoffLongitude();
        if (tlat == null || tlon == null) {
            return false;
        }
        Optional<TrackingPointDTO> last = Optional.ofNullable(getLastLocation(deliveryId));
        if (last.isEmpty() || last.get().getLatitude() == null) {
            return false;
        }
        double km = haversineKm(
                last.get().getLatitude().doubleValue(),
                last.get().getLongitude().doubleValue(),
                tlat.doubleValue(),
                tlon.doubleValue());
        return km * 1000.0 <= thresholdMeters;
    }

    @Override
    @Transactional(readOnly = true)
    public String getOptimizedRoute(Long deliveryId) {
        deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable: " + deliveryId));
        return "";
    }

    @Override
    @Transactional
    public void notifyCustomerOfApproach(Long deliveryId, int minutesAway) {
        log.info("notify approach deliveryId={} minutesAway={} (no-op notification bus)", deliveryId, minutesAway);
    }

    @Override
    @Transactional
    public void cleanupOldTrackingPoints(int daysToKeep) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(1, daysToKeep));
        int n = trackingPointRepository.deleteByTimestampBefore(before);
        log.info("Cleaned up {} tracking points older than {}", n, before);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_R_KM * c;
    }

    private TrackingPointDTO toPointDto(TrackingPoint t) {
        return TrackingPointDTO.builder()
                .id(t.getId())
                .deliveryId(t.getDeliveryId())
                .latitude(t.getLatitude())
                .longitude(t.getLongitude())
                .accuracy(t.getAccuracy())
                .speed(t.getSpeed())
                .bearing(t.getBearing())
                .deliveryStatus(t.getDeliveryStatus())
                .timestamp(t.getTimestamp())
                .build();
    }
}
