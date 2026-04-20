package com.speedline.delivery.dispatch.scheduler;

import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.service.DispatchCycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchCycleSchedulerTest {

    @Mock
    private DispatchZoneConfig zoneConfig;
    @Mock
    private DispatchCycleService cycleService;
    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    private DispatchCycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DispatchCycleScheduler(zoneConfig, cycleService, taskScheduler);
    }

    @Test
    void runAllZones_fanOutsOnTwoZones() {
        DispatchProperties.ZoneConfig z1 = new DispatchProperties.ZoneConfig();
        z1.setId(1L);
        z1.setMode(DispatchMode.AUTO);
        DispatchProperties.ZoneConfig z2 = new DispatchProperties.ZoneConfig();
        z2.setId(2L);
        z2.setMode(DispatchMode.SEMI_AUTO);

        when(zoneConfig.getActiveZones()).thenReturn(List.of(z1, z2));
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(taskScheduler).execute(any(Runnable.class));

        scheduler.runAllZones();

        verify(taskScheduler, times(2)).execute(any(Runnable.class));
        verify(cycleService).runCycle(1L);
        verify(cycleService).runCycle(2L);
    }

    @Test
    void runAllZones_skipsZoneWithNullId() {
        DispatchProperties.ZoneConfig z1 = new DispatchProperties.ZoneConfig();
        z1.setId(null);
        z1.setMode(DispatchMode.AUTO);

        when(zoneConfig.getActiveZones()).thenReturn(List.of(z1));

        scheduler.runAllZones();

        verify(taskScheduler, times(0)).execute(any(Runnable.class));
    }
}
