package com.speedline.delivery.dispatch.scheduler;

import com.speedline.delivery.dispatch.service.PartnerDelayHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartnerDelayScheduler {

    private final PartnerDelayHandler partnerDelayHandler;

    @Scheduled(fixedDelayString = "${dispatch.partner-delay.scheduler-interval-seconds:15}000")
    public void checkActivePartnerDelays() {
        partnerDelayHandler.processActiveDelays();
    }
}
