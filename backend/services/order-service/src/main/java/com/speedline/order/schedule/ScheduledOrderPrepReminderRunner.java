package com.speedline.order.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Déclenche périodiquement {@link ScheduledOrderPrepReminderService} (transaction séparée du scheduler).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledOrderPrepReminderRunner {

    private final ScheduledOrderPrepReminderService scheduledOrderPrepReminderService;

    @Value("${order.scheduled-prep-reminder.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${order.scheduled-prep-reminder.poll-interval-ms:60000}", initialDelayString = "45000")
    public void tick() {
        if (!enabled) {
            return;
        }
        try {
            final LocalDateTime now = LocalDateTime.now();
            scheduledOrderPrepReminderService.sendDueReminders(now);
            scheduledOrderPrepReminderService.autoStartPreparingWhenDue(now);
        } catch (Exception ex) {
            log.warn("Erreur batch rappels commandes planifiées", ex);
        }
    }
}
