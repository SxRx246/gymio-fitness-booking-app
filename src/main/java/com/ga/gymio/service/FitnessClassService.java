package com.ga.gymio.service;

import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.repository.FitnessClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FitnessClassService {
    private final FitnessClassRepository fitnessClassRepository;

    public FitnessClass createFitnessClass(FitnessClass fitnessClass){
            if (fitnessClass.getStartTime()
                    .isAfter(fitnessClass.getEndTime())) {
                throw new IllegalArgumentException(
                        "Start time must be before end time"
                );
            }

        if (fitnessClass.getCapacity() <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be greater than 0"
            );
        }
        return fitnessClassRepository.save(fitnessClass);
    }
}