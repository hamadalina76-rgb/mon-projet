package com.speedline.delivery.dispatch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * DISP-101: read-accessor over {@link DispatchProperties} zone list.
 * Day-1 the list is static (application.yml). Later this can be replaced
 * by a DB-backed zone registry without touching callers.
 *
 * <p>Per-zone {@link DispatchMode} can be overridden at runtime via Redis
 * ({@link DispatchZoneModeStore}).
 */
@Service
@RequiredArgsConstructor
public class DispatchZoneConfig {

    private final DispatchProperties properties;
    private final DispatchZoneModeStore modeStore;

    public List<DispatchProperties.ZoneConfig> getZones() {
        return properties.getZones();
    }

    public List<DispatchProperties.ZoneConfig> getActiveZones() {
        return properties.getZones().stream()
                .filter(z -> z.getId() != null)
                .filter(z -> getMode(z.getId()) != DispatchMode.MANUAL)
                .toList();
    }

    public Optional<DispatchProperties.ZoneConfig> findById(Long zoneId) {
        if (zoneId == null) return Optional.empty();
        return properties.getZones().stream()
                .filter(z -> zoneId.equals(z.getId()))
                .findFirst();
    }

    /**
     * Effective mode: Redis override if set, otherwise YAML per zone, otherwise {@link DispatchMode#AUTO}.
     */
    public DispatchMode getMode(Long zoneId) {
        if (zoneId == null) {
            return DispatchMode.AUTO;
        }
        Optional<DispatchMode> override = modeStore.getOverride(zoneId);
        if (override.isPresent()) {
            return override.get();
        }
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
