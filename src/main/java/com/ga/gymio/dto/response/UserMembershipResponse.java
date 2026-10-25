
package com.ga.gymio.dto.response;

import com.ga.gymio.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserMembershipResponse {

    private Long userId;

    private Long membershipPlanId;

    private String membershipPlanName;

    private User.MembershipStatus membershipStatus;

    private LocalDate membershipStartDate;

    private LocalDate membershipEndDate;

    private LocalDate membershipFrozenAt;
}
