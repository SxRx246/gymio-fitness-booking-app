package com.ga.gymio.service;

import com.ga.gymio.model.User;
import com.ga.gymio.repository.MembershipPlanRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserMembershipService {

    private final UserRepository userRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final AuditLogService auditLogService;


    @Transactional
    public void expireMemberships() {
        List<User> expiredUsers =
                userRepository.findByMembershipStatusAndMembershipEndDateBefore(
                        User.MembershipStatus.ACTIVE,
                        LocalDate.now()
                );

        for (User user : expiredUsers) {
            user.setMembershipStatus(User.MembershipStatus.EXPIRED);
        }

        userRepository.saveAll(expiredUsers);
    }

}
