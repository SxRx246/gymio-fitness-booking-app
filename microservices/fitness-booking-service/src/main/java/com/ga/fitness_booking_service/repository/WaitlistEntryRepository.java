package com.ga.fitness_booking_service.repository;

import com.ga.fitness_booking_service.model.WaitlistEntry;
import com.ga.fitness_booking_service.model.WaitlistEntry.Status;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository
        extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByCustomerIdAndFitnessClass_Id(
            Long customerId,
            Long fitnessClassId
    );

    List<WaitlistEntry> findAllByFitnessClass_IdAndStatusOrderByJoinedAtAsc(
            Long fitnessClassId,
            Status status
    );

    List<WaitlistEntry> findAllByCustomerIdOrderByJoinedAtDesc(
            Long customerId
    );

    Optional<WaitlistEntry> findByIdAndCustomerId(
            Long id,
            Long customerId
    );

    List<WaitlistEntry> findAllByStatusAndOfferExpiresAtLessThanEqual(
            Status status,
            LocalDateTime time
    );

    long countByFitnessClass_IdAndStatusAndOfferExpiresAtAfter(
            Long fitnessClassId,
            Status status,
            LocalDateTime time
    );

    boolean existsByCustomerIdAndFitnessClass_Id(
            Long customerId,
            Long fitnessClassId
    );
}
