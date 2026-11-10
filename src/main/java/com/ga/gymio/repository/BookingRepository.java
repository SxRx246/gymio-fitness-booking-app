package com.ga.gymio.repository;

import com.ga.gymio.model.Booking;
import com.ga.gymio.service.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByCustomerId(Long customerId, Pageable pageable);

    List<Booking> findByFitnessClassId(Long fitnessClassId);

    boolean existsByCustomerIdAndFitnessClassIdAndStatus(Long customerId, Long fitnessClassId, Booking.Status status);

    long countByFitnessClassIdAndStatus( Long fitnessClassId, Booking.Status status );

    List<Booking> findByFitnessClassIdAndStatus( Long fitnessClassId, Booking.Status status );
}
