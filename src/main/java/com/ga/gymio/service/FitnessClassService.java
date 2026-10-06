package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.exception.ForbiddenException;
import com.ga.gymio.exception.InformationNotFoundException;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.ga.gymio.dto.request.FitnessClassRequest;

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
            throw new ForbiddenException(
                    "Selected user must have a trainer role" );
        }

        if (trainer.getStatus() != User.Status.ACTIVE) {
            throw new ForbiddenException(
                    "Trainer is not active" );
        }

        if (!trainer.isEmailVerified()) {
            throw new ForbiddenException(
                    "Trainer email is not verified" );
        }

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.TRAINER
                && !trainer.getId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You can only create fitness classes for yourself"
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
                                "User with id " + trainerId + " not found"
                        )
                );

        if (trainer.getRole() != User.Role.TRAINER) {
            throw new IllegalArgumentException(
                    "User with id " + trainerId + " is not a trainer"
            );
        }


        return fitnessClassRepository.findByTrainer(trainer);
    }

    private User getCurrentUser(){
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        MyUserDetails myUserDetails =
                (MyUserDetails) authentication.getPrincipal();

        User currentUser = myUserDetails.getUser();

        return currentUser;
    }

    private void checkClassOwnership(FitnessClass fitnessClass) {

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == User.Role.TRAINER
                && fitnessClass.getTrainer().getId().equals(currentUser.getId())) {
            return;
        }

        throw new ForbiddenException(
                "You are not allowed to manage this fitness class"
        );
    }

    public FitnessClass updateFitnessClass(FitnessClassRequest request, Long id){
        logger.info("Updating fitness class with id {}", id);

        FitnessClass existingFitnessClass = fitnessClassRepository.findById(id).orElseThrow(
                () -> new InformationNotFoundException(
                        "Fitness class with id " + id + " not found"
                )
        );

        checkClassOwnership(existingFitnessClass);

        if (existingFitnessClass.getStatus() == FitnessClass.Status.COMPLETED
                || existingFitnessClass.getStatus() == FitnessClass.Status.CANCELLED
                || existingFitnessClass.getStatus() == FitnessClass.Status.IN_PROGRESS) {

            throw new IllegalArgumentException(
                    "Completed, cancelled, or in-progress classes cannot be updated"
            );
        }

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }

        if(getCurrentUser().getRole() == User.Role.ADMIN){
            User trainer = userRepository.findByEmail(request.getTrainerEmail()).
                    orElseThrow(() -> new InformationNotFoundException(
                            "trainer with email " + request.getTrainerEmail() + " is not found"
                    ));
            existingFitnessClass.setTrainer(trainer);
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

        checkClassOwnership(fitnessClass);

        if(!fitnessClass.getTrainer().getEmail().equalsIgnoreCase(getCurrentUser().getEmail())
                && getCurrentUser().getRole() == User.Role.TRAINER
        ){
            throw new ForbiddenException(
                    "You are only allowed to cancel your own fitness classes"
            );
        }

        if (fitnessClass.getStatus() == FitnessClass.Status.COMPLETED
                || fitnessClass.getStatus() == FitnessClass.Status.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Completed or in-progress fitness classes cannot be cancelled"
            );
        }

        if (fitnessClass.getStatus() == FitnessClass.Status.CANCELLED) {
            throw new IllegalArgumentException(
                    "Fitness class is already cancelled"
            );
        }


        fitnessClass.setStatus(FitnessClass.Status.CANCELLED);

        FitnessClass cancelledClass = fitnessClassRepository.save(fitnessClass);

        logger.info("Fitness class cancelled successfully with id {}", cancelledClass.getId());
    }
}