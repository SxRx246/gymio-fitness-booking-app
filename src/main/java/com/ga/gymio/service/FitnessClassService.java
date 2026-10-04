package com.ga.gymio.service;

import com.ga.gymio.exception.InformationNotFoundException;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.ga.gymio.dto.request.FitnessClassRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FitnessClassService {
    private final FitnessClassRepository fitnessClassRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(FitnessClassService.class);

    public FitnessClass createFitnessClass(FitnessClassRequest request) {
        logger.info("Creating fitness class: {}", request.getName());

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

        FitnessClass savedClass = fitnessClassRepository.save(fitnessClass);

        logger.info("Fitness class created successfully with id {}", savedClass.getId());

        return savedClass;
    }

    public FitnessClass getFitnessClass(Long id){
        logger.info("Retrieving fitness class with id {}", id);
        return fitnessClassRepository.findById(id).orElseThrow( () ->
                new InformationNotFoundException(
                        "Fitness Class with id " + id + " not found"
                )
        );
    }
    public List<FitnessClass> getFitnessClasses(Long trainerId){
        logger.info("Retrieving fitness classes of trainer {}", trainerId);
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Trainer not found"
                        )
                );

        return fitnessClassRepository.findByTrainer(trainer);
    }

    public FitnessClass updateFitnessClass(FitnessClassRequest request, Long id){
        logger.info("Updating fitness class with id {}", id);

        FitnessClass existingFitnessClass = fitnessClassRepository.findById(id).orElseThrow(
                () -> new InformationNotFoundException(
                        "Fitness class with id " + id + " not found"
                )
        );

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }

        existingFitnessClass.setName(request.getName());
        existingFitnessClass.setDescription(request.getDescription());
        existingFitnessClass.setCapacity(request.getCapacity());
        existingFitnessClass.setStartTime(request.getStartTime());
        existingFitnessClass.setEndTime(request.getEndTime());
        existingFitnessClass.setLevel(request.getLevel());
        existingFitnessClass.setType(request.getType());

        FitnessClass updatedClass = fitnessClassRepository.save(existingFitnessClass);

        logger.info("Fitness class updated successfully with id {}", updatedClass.getId());

        return updatedClass;
    }

    public void cancelFitnessClass(Long id){
        logger.info("Cancelling fitness class with id {}", id);

        FitnessClass fitnessClass = fitnessClassRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException(
                        "Fitness Class with id " + id + " not found"
                ));

        fitnessClass.setStatus(FitnessClass.Status.CANCELLED);

        FitnessClass cancelledClass =
                fitnessClassRepository.save(fitnessClass);

        logger.info("Fitness class cancelled successfully with id {}", cancelledClass.getId());
    }
}