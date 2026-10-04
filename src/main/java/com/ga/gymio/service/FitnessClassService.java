package com.ga.gymio.service;

import com.ga.gymio.exception.InformationNotFoundException;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FitnessClassService {
    private final FitnessClassRepository fitnessClassRepository;
    private final UserRepository userRepository;

    public FitnessClass createFitnessClass(FitnessClass fitnessClass){
        User trainer = userRepository
                .findByEmail(fitnessClass.getTrainer().getEmail())
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Trainer not found"
                        )
                );

        if (trainer.getRole() != User.Role.TRAINER) {
            throw new InformationNotFoundException(
                    "User must have a trainer role"
            );
        }

            if (fitnessClass.getStartTime()
                    .isAfter(fitnessClass.getEndTime())) {
                throw new IllegalArgumentException(
                        "Start time must be before end time"
                );
            }

            if (!fitnessClass.getStartTime().isAfter(LocalDateTime.now())){
                throw new IllegalArgumentException(
                        "Start time must be in the future"
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