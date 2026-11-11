package com.ga.fitness_booking_service.service;

import com.ga.fitness_booking_service.client.UserServiceClient;
import com.ga.fitness_booking_service.dto.AuthenticatedUser;
import com.ga.fitness_booking_service.dto.request.FitnessClassRequest;
import com.ga.fitness_booking_service.dto.response.TrainerValidationResponse;
import com.ga.fitness_booking_service.event.CustomerNotificationEvent;
import com.ga.fitness_booking_service.exception.ForbiddenException;
import com.ga.fitness_booking_service.exception.InformationNotFoundException;
import com.ga.fitness_booking_service.model.AuditLog;
import com.ga.fitness_booking_service.model.Booking;
import com.ga.fitness_booking_service.model.FitnessClass;
import com.ga.fitness_booking_service.repository.BookingRepository;
import com.ga.fitness_booking_service.repository.FitnessClassRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages fitness classes, including creation, retrieval, updates,
 * cancellation, deletion, and automatic status transitions.
 */
@Service
@RequiredArgsConstructor
public class FitnessClassService {

    private final FitnessClassRepository fitnessClassRepository;
    private final BookingRepository bookingRepository;
    private final UserServiceClient userServiceClient;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    private static final Logger logger =
            LoggerFactory.getLogger(FitnessClassService.class);

    /**
     * Validates that the selected trainer has an active account,
     * a trainer role, and a verified email address.
     *
     * @param trainer the trainer information returned by User Service
     */
    private void validateTrainer(TrainerValidationResponse trainer) {

        if (!"TRAINER".equals(trainer.role())) {
            throw new ForbiddenException(
                    "Selected user must have a trainer role");
        }

        if (!"ACTIVE".equals(trainer.status())) {
            throw new ForbiddenException("Trainer is not active");
        }

        if (!trainer.emailVerified()) {
            throw new ForbiddenException("Trainer email is not verified");
        }
    }

    /**
     * Retrieves and validates a trainer using their email address.
     *
     * @param email the trainer's email address
     * @return validated trainer information
     */
    private TrainerValidationResponse getValidatedTrainer(String email) {

        TrainerValidationResponse trainer =
                userServiceClient.validateTrainerByEmail(email)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Trainer not found or not eligible"));

        validateTrainer(trainer);

        return trainer;
    }

    /**
     * Creates a fitness class after validating the trainer and class schedule.
     *
     * @param request the fitness class details
     * @return the saved fitness class
     */
    @Transactional
    public FitnessClass createFitnessClass(FitnessClassRequest request) {

        logger.info("Creating fitness class: {}", request.getName());

        AuthenticatedUser currentUser = getCurrentUser();

        TrainerValidationResponse trainer =
                getValidatedTrainer(request.getTrainerEmail());

        if ("TRAINER".equals(currentUser.role())
                && !trainer.id().equals(currentUser.userId())) {
            throw new ForbiddenException(
                    "You can only create fitness classes for yourself");
        }

        if (!"ADMIN".equals(currentUser.role())
                && !"TRAINER".equals(currentUser.role())) {
            throw new ForbiddenException(
                    "You are not allowed to create fitness classes");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException(
                    "Start time must be before end time");
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
        fitnessClass.setTrainerId(trainer.id());

        FitnessClass savedClass =
                fitnessClassRepository.save(fitnessClass);

        auditLogService.log(
                AuditLog.Action.CREATE_CLASS,
                "User " + currentUser.userId()
                        + " created Fitness Class " + savedClass.getId(),
                currentUser.userId());

        logger.info(
                "Fitness class created successfully with id {}",
                savedClass.getId());

        return savedClass;
    }

    /**
     * Retrieves a fitness class by its ID.
     *
     * @param id the fitness class ID
     * @return the requested fitness class
     */
    @Transactional(readOnly = true)
    public FitnessClass getFitnessClass(Long id) {

        logger.info("Retrieving fitness class with id {}", id);

        return fitnessClassRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Fitness Class with id " + id + " not found"));
    }

    /**
     * Retrieves a trainer's fitness classes in paginated form.
     *
     * @param trainerId the trainer's user ID
     * @param pageable pagination and sorting information
     * @return a page of the trainer's fitness classes
     */
    @Transactional(readOnly = true)
    public Page<FitnessClass> getFitnessClasses(
            Long trainerId,
            Pageable pageable) {

        logger.info(
                "Retrieving fitness classes of trainer {}",
                trainerId);

        return fitnessClassRepository.findByTrainerId(
                trainerId, pageable);
    }

    /**
     * Searches fitness classes using optional type, level, and status filters.
     *
     * @param type the class type filter, or null to include all types
     * @param level the difficulty level filter, or null to include all levels
     * @param status the class status filter, or null to include all statuses
     * @param pageable pagination and sorting information
     * @return a page of matching fitness classes
     */
    @Transactional(readOnly = true)
    public Page<FitnessClass> searchFitnessClasses(
            FitnessClass.Type type,
            FitnessClass.Level level,
            FitnessClass.Status status,
            Pageable pageable) {

        logger.info(
                "Searching fitness classes with type={}, level={}, status={}",
                type, level, status);

        return fitnessClassRepository.findByFilters(
                type, level, status, pageable);
    }

    /**
     * Retrieves the authenticated user from the security context.
     *
     * @return the authenticated user's identity and role
     */
    private AuthenticatedUser getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof AuthenticatedUser currentUser)) {
            throw new ForbiddenException(
                    "Authenticated user information is unavailable");
        }

        return currentUser;
    }

    /**
     * Checks whether the authenticated user is allowed to manage a class.
     * Administrators can manage all classes, while trainers can manage
     * only their own classes.
     *
     * @param fitnessClass the fitness class to check
     */
    private void checkClassOwnership(FitnessClass fitnessClass) {

        AuthenticatedUser currentUser = getCurrentUser();

        if ("ADMIN".equals(currentUser.role())) {
            return;
        }

        if ("TRAINER".equals(currentUser.role())
                && fitnessClass.getTrainerId() != null
                && fitnessClass.getTrainerId().equals(currentUser.userId())) {
            return;
        }

        throw new ForbiddenException(
                "You are not allowed to manage this fitness class");
    }

    /**
     * Publishes an in-app notification request for a customer.
     * The NotificationEventPublisher forwards this event to Kafka.
     *
     * @param customerId the customer receiving the notification
     * @param message the notification message
     */
    private void publishCustomerNotification(
            Long customerId,
            String message) {

        eventPublisher.publishEvent(
                new CustomerNotificationEvent(customerId, message));
    }

    /**
     * Updates a fitness class after checking ownership and its current status.
     * Confirmed customers are notified through the Notification Service.
     *
     * @param request the updated fitness class details
     * @param id the ID of the fitness class to update
     * @return the updated fitness class
     */
    @Transactional
    public FitnessClass updateFitnessClass(
            FitnessClassRequest request,
            Long id) {

        logger.info("Updating fitness class with id {}", id);

        AuthenticatedUser currentUser = getCurrentUser();

        FitnessClass existingFitnessClass =
                fitnessClassRepository.findById(id)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Fitness class with id "
                                                + id + " not found"));

        checkClassOwnership(existingFitnessClass);

        if (existingFitnessClass.getStatus() == FitnessClass.Status.COMPLETED
                || existingFitnessClass.getStatus() == FitnessClass.Status.CANCELLED
                || existingFitnessClass.getStatus() == FitnessClass.Status.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Completed, cancelled, or in-progress classes cannot be updated");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException(
                    "Start time must be before end time");
        }

        if ("ADMIN".equals(currentUser.role())) {

            TrainerValidationResponse trainer =
                    getValidatedTrainer(request.getTrainerEmail());

            existingFitnessClass.setTrainerId(trainer.id());

        } else if (!"TRAINER".equals(currentUser.role())) {

            throw new ForbiddenException(
                    "You are not allowed to update this fitness class");
        }

        existingFitnessClass.setName(request.getName());
        existingFitnessClass.setDescription(request.getDescription());
        existingFitnessClass.setCapacity(request.getCapacity());
        existingFitnessClass.setStartTime(request.getStartTime());
        existingFitnessClass.setEndTime(request.getEndTime());
        existingFitnessClass.setLevel(request.getLevel());
        existingFitnessClass.setType(request.getType());

        FitnessClass updatedClass =
                fitnessClassRepository.save(existingFitnessClass);

        List<Booking> confirmedBookings =
                bookingRepository.findByFitnessClassIdAndStatus(
                        updatedClass.getId(),
                        Booking.Status.CONFIRMED);

        for (Booking booking : confirmedBookings) {
            publishCustomerNotification(
                    booking.getCustomerId(),
                    "Fitness Class " + updatedClass.getName()
                            + " has been updated");
        }

        auditLogService.log(
                AuditLog.Action.UPDATE_CLASS,
                "User " + currentUser.userId()
                        + " updated Fitness Class " + updatedClass.getId(),
                currentUser.userId());

        logger.info(
                "Fitness class updated successfully with id {}",
                updatedClass.getId());

        return updatedClass;
    }

    /**
     * Cancels a fitness class and updates its confirmed bookings.
     * Customers receive in-app notifications through the Notification Service.
     *
     * @param id the ID of the fitness class to cancel
     */
    @Transactional
    public void cancelFitnessClass(Long id) {

        logger.info("Cancelling fitness class with id {}", id);

        AuthenticatedUser currentUser = getCurrentUser();

        FitnessClass fitnessClass =
                fitnessClassRepository.findById(id)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Fitness Class with id "
                                                + id + " not found"));

        checkClassOwnership(fitnessClass);

        if (fitnessClass.getStatus() == FitnessClass.Status.COMPLETED
                || fitnessClass.getStatus() == FitnessClass.Status.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Completed or in-progress fitness classes cannot be cancelled");
        }

        if (fitnessClass.getStatus() == FitnessClass.Status.CANCELLED) {
            throw new IllegalArgumentException(
                    "Fitness class is already cancelled");
        }

        List<Booking> confirmedBookings =
                bookingRepository.findByFitnessClassIdAndStatus(
                        fitnessClass.getId(),
                        Booking.Status.CONFIRMED);

        fitnessClass.setStatus(FitnessClass.Status.CANCELLED);

        FitnessClass cancelledClass =
                fitnessClassRepository.save(fitnessClass);

        auditLogService.log(
                AuditLog.Action.CANCEL_CLASS,
                "User " + currentUser.userId()
                        + " cancelled Fitness Class " + cancelledClass.getId(),
                currentUser.userId());

        for (Booking booking : confirmedBookings) {

            booking.setStatus(Booking.Status.CANCELLED);

            publishCustomerNotification(
                    booking.getCustomerId(),
                    "Fitness Class " + fitnessClass.getName()
                            + " has been cancelled");

            logger.info(
                    "Booking {} cancelled because fitness class {} was cancelled",
                    booking.getId(),
                    fitnessClass.getId());
        }

        bookingRepository.saveAll(confirmedBookings);

        logger.info(
                "Fitness class cancelled successfully with id {}",
                cancelledClass.getId());
    }

    /**
     * Periodically updates fitness class and booking statuses
     * according to their scheduled start and end times.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateFitnessClassStatuses() {

        LocalDateTime now = LocalDateTime.now();

        List<FitnessClass> scheduledClasses =
                fitnessClassRepository.findByStatusAndStartTimeLessThanEqual(
                        FitnessClass.Status.SCHEDULED,
                        now);

        for (FitnessClass fitnessClass : scheduledClasses) {

            if (!now.isBefore(fitnessClass.getEndTime())) {

                fitnessClass.setStatus(FitnessClass.Status.COMPLETED);

                updateBookingStatuses(fitnessClass);

                auditLogService.log(
                        AuditLog.Action.COMPLETE_CLASS,
                        "Fitness Class " + fitnessClass.getId()
                                + " was completed",
                        null);

                logger.info(
                        "Fitness class {} has been completed",
                        fitnessClass.getId());

            } else {

                fitnessClass.setStatus(FitnessClass.Status.IN_PROGRESS);

                logger.info(
                        "Fitness class {} is now in progress",
                        fitnessClass.getId());
            }
        }

        fitnessClassRepository.saveAll(scheduledClasses);

        List<FitnessClass> inProgressClasses =
                fitnessClassRepository.findByStatusAndEndTimeLessThanEqual(
                        FitnessClass.Status.IN_PROGRESS,
                        now);

        for (FitnessClass fitnessClass : inProgressClasses) {

            updateBookingStatuses(fitnessClass);

            fitnessClass.setStatus(FitnessClass.Status.COMPLETED);

            auditLogService.log(
                    AuditLog.Action.COMPLETE_CLASS,
                    "Fitness Class " + fitnessClass.getId()
                            + " was completed",
                    null);

            logger.info(
                    "Fitness class {} has been completed",
                    fitnessClass.getId());
        }

        fitnessClassRepository.saveAll(inProgressClasses);
    }

    /**
     * Completes all confirmed bookings associated with a fitness class.
     * Customers receive an in-app notification through the Notification Service.
     *
     * @param fitnessClass the completed fitness class
     */
    private void updateBookingStatuses(FitnessClass fitnessClass) {

        List<Booking> confirmedBookings =
                bookingRepository.findByFitnessClassIdAndStatus(
                        fitnessClass.getId(),
                        Booking.Status.CONFIRMED);

        for (Booking booking : confirmedBookings) {

            booking.setStatus(Booking.Status.COMPLETED);

            auditLogService.log(
                    AuditLog.Action.COMPLETE_BOOKING,
                    "Booking " + booking.getId() + " was completed",
                    null);

            publishCustomerNotification(
                    booking.getCustomerId(),
                    "Fitness Class " + fitnessClass.getName()
                            + " has been completed");

            logger.info(
                    "Booking {} has been completed",
                    booking.getId());
        }

        bookingRepository.saveAll(confirmedBookings);
    }

    /**
     * Deletes a fitness class and records the action in the audit log.
     *
     * @param id the ID of the fitness class to delete
     */
    @Transactional
    public void deleteFitnessClass(Long id) {

        logger.info("Deleting fitness class with id {}", id);

        AuthenticatedUser currentUser = getCurrentUser();

        FitnessClass fitnessClass =
                fitnessClassRepository.findById(id)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Fitness Class with id "
                                                + id + " not found"));

        checkClassOwnership(fitnessClass);

        fitnessClassRepository.delete(fitnessClass);

        auditLogService.log(
                AuditLog.Action.DELETE_CLASS,
                "User " + currentUser.userId()
                        + " deleted Fitness Class " + id,
                currentUser.userId());

        logger.info(
                "Fitness class {} deleted successfully",
                id);
    }
}
