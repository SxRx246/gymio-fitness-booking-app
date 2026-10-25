package com.ga.gymio.service;

import com.ga.gymio.model.User;
import com.ga.gymio.repository.MembershipPlanRepository;
import com.ga.gymio.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMembershipServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipPlanRepository membershipPlanRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserMembershipService userMembershipService;

    @Test
    void expireMemberships_shouldMarkExpiredUsersAsExpired() {
        User user = new User();
        user.setMembershipStatus(User.MembershipStatus.ACTIVE);

        when(userRepository
                .findByMembershipStatusAndMembershipEndDateBefore(
                        User.MembershipStatus.ACTIVE,
                        LocalDate.now()
                ))
                .thenReturn(List.of(user));

        userMembershipService.expireMemberships();

       assertEquals(
                User.MembershipStatus.EXPIRED,
                user.getMembershipStatus()
        );

        verify(userRepository).saveAll(List.of(user));
    }
}