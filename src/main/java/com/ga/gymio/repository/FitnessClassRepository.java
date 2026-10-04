package com.ga.gymio.repository;

import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FitnessClassRepository extends JpaRepository<FitnessClass,Long> {
    List<FitnessClass> findByTrainer (User trainer);
}
