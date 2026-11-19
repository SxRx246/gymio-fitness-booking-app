package com.ga.membership_payment_service.dto.response;

import com.ga.membership_payment_service.model.UserMembership;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserMembershipResponse {

    private Long userId;

    private Long membershipPlanId;

    private String membershipPlanName;

    private UserMembership.MembershipStatus membershipStatus;

    private LocalDate membershipStartDate;

    private LocalDate membershipEndDate;

    private LocalDate membershipFrozenAt;
}
