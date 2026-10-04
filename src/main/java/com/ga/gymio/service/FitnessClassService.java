package com.ga.gymio.service;

import com.ga.gymio.exception.InformationNotFoundException;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ga.gymio.dto.request.FitnessClassRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FitnessClassService {
    private final FitnessClassRepository fitnessClassRepository;
    private final UserRepository userRepository;

    public FitnessClass createFitnessClass(FitnessClassRequest request) {
        User trainer = userRepository
                .findByEmail(request.getTrainerEmail())
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

        if (request.getStartTime()
                .isAfter(request.getEndTime())) {
            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }

        FitnessClass fitnessClass = new FitnessClass();

        fitnessClass.setName(request.getName());
        fitnessClass.setDescription(request.getDescription());
        fitnessClass.setType(request.getType());
        fitnessClass.setLevel(request.getLevel());
        fitnessClass.setStartTime(request.getStartTime());
        fitnessClass.setEndTime(request.getEndTime());
        fitnessClass.setCapacity(request.getCapacity());
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setTrainer(trainer);

        return fitnessClassRepository.save(fitnessClass);
    }

    public FitnessClass getFitnessClass(Long id){
        return fitnessClassRepository.findById(id).orElseThrow( () ->
                new InformationNotFoundException(
                        "Fitness Class with id " + id + " not found"
                )
        );
    }
    public List<FitnessClass> getFitnessClasses(Long trainerId){
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Trainer not found"
                        )
                );

        return fitnessClassRepository.findByTrainer(trainer);
    }
}