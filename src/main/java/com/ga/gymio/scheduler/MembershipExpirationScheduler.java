package com.ga.gymio.scheduler;

import com.ga.gymio.service.UserMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipExpirationScheduler {

    private final UserMembershipService userMembershipService;

    @Scheduled(
            cron = "0 0 0 * * *",
            zone = "Asia/Bahrain"
    )
    public void expireMembershipsDaily() {
        userMembershipService.expireMemberships();
    }
}