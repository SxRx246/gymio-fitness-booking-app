package com.ga.membership_payment_service.service;

import com.ga.membership_payment_service.model.UserMembership;
import com.ga.membership_payment_service.repository.UserMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMembershipServiceTest {

    @Mock
    private UserMembershipRepository userMembershipRepository;

    @InjectMocks
    private UserMembershipService userMembershipService;

    @Test
    void expireMemberships_shouldMarkExpiredMembershipsAsExpired() {
        UserMembership membership = new UserMembership();
        membership.setId(1L);
        membership.setUserId(10L);
        membership.setMembershipStatus(
                UserMembership.MembershipStatus.ACTIVE
        );
        membership.setMembershipEndDate(LocalDate.now().minusDays(1));

        when(userMembershipRepository
                .findByMembershipStatusAndMembershipEndDateBefore(
                        UserMembership.MembershipStatus.ACTIVE,
                        LocalDate.now()
                ))
                .thenReturn(List.of(membership));

        userMembershipService.expireMemberships();

        assertEquals(
                UserMembership.MembershipStatus.EXPIRED,
                membership.getMembershipStatus()
        );

        verify(userMembershipRepository)
                .findByMembershipStatusAndMembershipEndDateBefore(
                        UserMembership.MembershipStatus.ACTIVE,
                        LocalDate.now()
                );

        verify(userMembershipRepository).saveAll(List.of(membership));
        verifyNoMoreInteractions(userMembershipRepository);
    }

    @Test
    void expireMemberships_shouldSaveEmptyListWhenNoMembershipsAreExpired() {
        when(userMembershipRepository
                .findByMembershipStatusAndMembershipEndDateBefore(
                        UserMembership.MembershipStatus.ACTIVE,
                        LocalDate.now()
                ))
                .thenReturn(List.of());

        userMembershipService.expireMemberships();

        verify(userMembershipRepository).saveAll(List.of());
        verifyNoMoreInteractions(userMembershipRepository);
    }
}
