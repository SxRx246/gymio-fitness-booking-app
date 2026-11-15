package com.ga.membership_payment_service.repository;

import com.ga.membership_payment_service.model.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    boolean existsByName(String name);

    Optional<MembershipPlan> findByName(String name);

    List<MembershipPlan> findAllByActiveTrue();
}
