package com.ga.gymio.scheduler;

import com.ga.gymio.service.WaitlistEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs a scheduled task to check for expired waitlist offers.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
public class WaitlistOfferExpiryScheduler {

    private final WaitlistEntryService waitlistEntryService;

    /**
     * Checks for expired offers every minute.
     */
    @Scheduled(
            fixedDelayString = "${gymio.waitlist.expiry-check-ms:60000}"
    )
    public void expireOffers() {
        waitlistEntryService.expireOffersAndNotifyNext();
    }
}