package com.ga.membership_payment_service.service;

import com.ga.membership_payment_service.model.UserMembership;
import com.ga.membership_payment_service.repository.UserMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserMembershipService {

    private final UserMembershipRepository userMembershipRepository;

    @Transactional
    public void expireMemberships() {

        List<UserMembership> expiredMemberships =
                userMembershipRepository
                        .findByMembershipStatusAndMembershipEndDateBefore(
                                UserMembership.MembershipStatus.ACTIVE,
                                LocalDate.now()
                        );

        for (UserMembership membership : expiredMemberships) {
            membership.setMembershipStatus(UserMembership.MembershipStatus.EXPIRED);
        }

        userMembershipRepository.saveAll(expiredMemberships);
    }
}
