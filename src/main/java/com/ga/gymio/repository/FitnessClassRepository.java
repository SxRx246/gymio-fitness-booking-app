package com.ga.gymio.repository;

import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FitnessClassRepository extends JpaRepository<FitnessClass,Long> {
    Page<FitnessClass> findByTrainer(User trainer, Pageable pageable);

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

    List<FitnessClass> findByStatusAndStartTimeLessThanEqual( FitnessClass.Status status, LocalDateTime time );

    List<FitnessClass> findByStatusAndEndTimeLessThanEqual( FitnessClass.Status status, LocalDateTime time );
}
