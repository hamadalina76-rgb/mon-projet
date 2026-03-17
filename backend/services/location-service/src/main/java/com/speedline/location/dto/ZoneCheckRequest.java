package com.speedline.location.dto;

public record ZoneCheckRequest(
        double lat,
        double lng,
        Long zoneId
) {
}

