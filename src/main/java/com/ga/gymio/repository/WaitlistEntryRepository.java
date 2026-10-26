package com.ga.gymio.repository;

import com.ga.gymio.model.WaitlistEntry;
import com.ga.gymio.model.WaitlistEntry.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {
    Optional<WaitlistEntry> findByCustomer_IdAndFitnessClass_Id(Long customerId, Long fitnessClassId);

    List<WaitlistEntry> findAllByFitnessClass_IdAndStatusOrderByJoinedAtAsc(Long fitnessClassId, Status status);

    List<WaitlistEntry> findAllByCustomer_IdOrderByJoinedAtDesc(Long customerId);

    Optional<WaitlistEntry> findByIdAndCustomer_Id(Long id, Long customerId);

    boolean existsByCustomer_IdAndFitnessClass_Id(Long customerId, Long fitnessClassId);
}