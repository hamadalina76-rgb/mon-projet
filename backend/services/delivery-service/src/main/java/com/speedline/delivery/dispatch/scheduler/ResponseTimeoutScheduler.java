package com.speedline.delivery.dispatch.scheduler;

import com.speedline.delivery.dispatch.service.CourierResponseTimeoutTracker;
import com.speedline.delivery.dispatch.service.RefusalHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ResponseTimeoutScheduler {

    private final CourierResponseTimeoutTracker timeoutTracker;
    private final RefusalHandler refusalHandler;

    @Scheduled(fixedDelayString = "${dispatch.response-timeout.poll-interval-ms:5000}")
    public void checkTimeouts() {
        for (CourierResponseTimeoutTracker.ProposalRef ref : timeoutTracker.findExpired(Instant.now())) {
            refusalHandler.handleRefusal(ref.getOrderId(), ref.getCourierId(), "TIMEOUT");
            timeoutTracker.removeDeadline(ref.getOrderId(), ref.getCourierId());
        }
    }
}
