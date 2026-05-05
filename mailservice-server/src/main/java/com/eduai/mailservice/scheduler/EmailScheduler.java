package com.eduai.mailservice.scheduler;

import com.eduai.mailservice.service.mail.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Spring Scheduler trigger for due scheduled emails.
 * Polls the database every minute for pending scheduled emails.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailScheduler {

    private final SchedulerService schedulerService;

    /**
     * Process due scheduled emails every 60 seconds.
     */
    @Scheduled(fixedRateString = "${mail.scheduler.poll-rate-ms:60000}")
    public void processScheduledEmails() {
        log.debug("EmailScheduler: checking for due scheduled emails");
        try {
            schedulerService.processDueEmails();
        } catch (Exception ex) {
            log.error("EmailScheduler: error processing due emails: {}", ex.getMessage(), ex);
        }
    }
}
