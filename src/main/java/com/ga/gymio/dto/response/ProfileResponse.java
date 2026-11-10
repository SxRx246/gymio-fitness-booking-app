package com.ga.gymio.dto.response;

import com.ga.gymio.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String imageUrl;

    private String membershipPlanName;
    private User.MembershipStatus membershipStatus;
    private LocalDate membershipStartDate;
    private LocalDate membershipEndDate;
}