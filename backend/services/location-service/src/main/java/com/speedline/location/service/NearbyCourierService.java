package com.speedline.location.service;

import com.speedline.location.dto.CourierPositionDto;
import com.speedline.location.dto.NearbyCourierDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NearbyCourierService {

    private final CourierPositionRedisService positionRedisService;

    public List<NearbyCourierDto> findNearby(double centerLat, double centerLng, double radiusKm) {
        List<CourierPositionDto> online = positionRedisService.getAllOnline();

        return online.stream()
                .map(p -> {
                    double distanceKm = haversineKm(centerLat, centerLng, p.lat(), p.lng());
                    return new NearbyCourierDto(
                            p.courierId(),
                            p.lat(),
                            p.lng(),
                            distanceKm,
                            p.speed(),
                            p.heading(),
                            p.lastUpdatedAt()
                    );
                })
                .filter(dto -> dto.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyCourierDto::distanceKm))
                .toList();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

