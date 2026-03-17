package com.speedline.location.dto;

public record NearbyCourierDto(
        String courierId,
        double lat,
        double lng,
        double distanceKm,
        Double speed,
        Double heading,
        String lastUpdatedAt
) {
}

