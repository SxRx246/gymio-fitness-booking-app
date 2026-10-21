package com.ga.gymio.repository;

import com.ga.gymio.model.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    boolean existsByName(String name);

    Optional<MembershipPlan> findByName(String name);
}
