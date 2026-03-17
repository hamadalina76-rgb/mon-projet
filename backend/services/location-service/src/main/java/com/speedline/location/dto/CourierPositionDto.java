package com.speedline.location.dto;

public record CourierPositionDto(
        String courierId,
        double lat,
        double lng,
        Double speed,
        Double heading,
        String lastUpdatedAt
) {
}

