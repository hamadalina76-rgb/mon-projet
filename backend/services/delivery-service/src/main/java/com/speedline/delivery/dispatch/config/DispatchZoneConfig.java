package com.speedline.delivery.dispatch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * DISP-101: read-accessor over {@link DispatchProperties} zone list.
 * Day-1 the list is static (application.yml). Later this can be replaced
 * by a DB-backed zone registry without touching callers.
 */
@Service
@RequiredArgsConstructor
public class DispatchZoneConfig {

    private final DispatchProperties properties;

    public List<DispatchProperties.ZoneConfig> getZones() {
        return properties.getZones();
    }

    public List<DispatchProperties.ZoneConfig> getActiveZones() {
        return properties.getZones().stream()
                .filter(z -> z.getMode() != DispatchMode.MANUAL)
                .toList();
    }

    public Optional<DispatchProperties.ZoneConfig> findById(Long zoneId) {
        if (zoneId == null) return Optional.empty();
        return properties.getZones().stream()
                .filter(z -> zoneId.equals(z.getId()))
                .findFirst();
    }

    public DispatchMode getMode(Long zoneId) {
        return findById(zoneId).map(DispatchProperties.ZoneConfig::getMode).orElse(DispatchMode.AUTO);
    }

    public int getMaxCapacity(Long zoneId) {
        return findById(zoneId).map(DispatchProperties.ZoneConfig::getMaxCapacity).orElse(50);
    }

    public int getInternalShortageThreshold(Long zoneId) {
        return findById(zoneId)
                .map(DispatchProperties.ZoneConfig::getInternalShortageThreshold)
                .filter(v -> v != null && v > 0)
                .orElse(properties.getEligibility().getInternalShortageThreshold());
    }
}
