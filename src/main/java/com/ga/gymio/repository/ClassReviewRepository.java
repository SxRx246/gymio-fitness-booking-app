package com.ga.gymio.repository;

import com.ga.gymio.model.ClassReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ClassReviewRepository
        extends JpaRepository<ClassReview, Long> {

    Optional<ClassReview> findByBooking_Id(Long bookingId);

    boolean existsByBooking_Id(Long bookingId);

    List<ClassReview> findAllByBooking_FitnessClass_IdOrderByCreatedAtDesc(
            Long fitnessClassId
    );
}