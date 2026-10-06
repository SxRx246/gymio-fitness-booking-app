package com.ga.gymio.repository;

import com.ga.gymio.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerId(Long customerId);

    boolean existsByCustomerIdAndFitnessClassId(
            Long customerId,
            Long fitnessClassId
    );
}
