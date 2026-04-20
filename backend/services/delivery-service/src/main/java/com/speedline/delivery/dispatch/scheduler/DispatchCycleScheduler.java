package com.speedline.delivery.dispatch.scheduler;

import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.service.DispatchCycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchCycleScheduler {

    private final DispatchZoneConfig zoneConfig;
    private final DispatchCycleService dispatchCycleService;
    private final ThreadPoolTaskScheduler dispatchTaskScheduler;

    @Scheduled(fixedDelayString = "${dispatch.interval-seconds}000")
    public void runAllZones() {
        for (var zone : zoneConfig.getActiveZones()) {
            if (zone.getId() == null) {
                continue;
            }
            dispatchTaskScheduler.execute(() -> {
                try {
                    dispatchCycleService.runCycle(zone.getId());
                } catch (Exception ex) {
                    log.error("Dispatch scheduler task failed zoneId={}: {}", zone.getId(), ex.getMessage(), ex);
                }
            });
        }
    }
}
