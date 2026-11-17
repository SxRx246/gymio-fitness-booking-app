package com.ga.fitness_booking_service.repository;

import com.ga.fitness_booking_service.model.FitnessClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FitnessClassRepository
        extends JpaRepository<FitnessClass, Long> {

    Page<FitnessClass> findByTrainerId(Long trainerId, Pageable pageable);

    @Query("""
            SELECT f FROM FitnessClass f
            WHERE (:type IS NULL OR f.type = :type)
            AND (:level IS NULL OR f.level = :level)
            AND (:status IS NULL OR f.status = :status)
            """)
    Page<FitnessClass> findByFilters(
            @Param("type") FitnessClass.Type type,
            @Param("level") FitnessClass.Level level,
            @Param("status") FitnessClass.Status status,
            Pageable pageable
    );

    List<FitnessClass> findByStatusAndStartTimeLessThanEqual(
            FitnessClass.Status status,
            LocalDateTime time
    );

    List<FitnessClass> findByStatusAndEndTimeLessThanEqual(
            FitnessClass.Status status,
            LocalDateTime time
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FitnessClass f WHERE f.id = :id")
    Optional<FitnessClass> findByIdForUpdate(@Param("id") Long id);

    boolean existsByNameAndStartTimeAndTrainerId(
            String name,
            LocalDateTime startTime,
            Long trainerId
    );

    boolean existsByNameAndTrainerId(String name, Long trainerId);
}
