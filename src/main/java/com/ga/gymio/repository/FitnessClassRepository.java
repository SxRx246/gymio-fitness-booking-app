package com.ga.gymio.repository;

import com.ga.gymio.model.FitnessClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FitnessClassRepository extends JpaRepository<FitnessClass, Long> {
    FitnessClass findByUserId(Long id);
}
