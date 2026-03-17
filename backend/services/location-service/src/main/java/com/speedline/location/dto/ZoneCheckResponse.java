package com.speedline.location.dto;

public record ZoneCheckResponse(
        boolean inZone,
        Long zoneId
) {
}

