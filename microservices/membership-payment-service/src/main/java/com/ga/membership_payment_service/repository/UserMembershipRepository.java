package com.ga.membership_payment_service.repository;

import com.ga.membership_payment_service.model.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {

    List<UserMembership> findByUserId(Long userId);

    Optional<UserMembership> findByUserIdAndMembershipStatus(
            Long userId,
            UserMembership.MembershipStatus membershipStatus
    );
}