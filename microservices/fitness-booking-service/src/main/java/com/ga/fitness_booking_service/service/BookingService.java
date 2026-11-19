package com.ga.fitness_booking_service.service;

import com.ga.fitness_booking_service.dto.AuthenticatedUser;
import com.ga.fitness_booking_service.dto.response.BookingResponse;
import com.ga.fitness_booking_service.event.BookingConfirmedEvent;
import com.ga.fitness_booking_service.event.CustomerNotificationEvent;
import com.ga.fitness_booking_service.exception.ForbiddenException;
import com.ga.fitness_booking_service.exception.InformationExistsException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles booking operations, including creation, retrieval, cancellation,
 * and deletion, while enforcing booking rules and access permissions.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final WaitlistEntryService waitlistEntryService;

    private static final Logger logger =
            LoggerFactory.getLogger(BookingService.class);

    /**
     * Creates a booking if the fitness class is available and has capacity.
     *
     * @param fitnessClassId the ID of the fitness class to book
     * @return the created booking details
     * @throws InformationNotFoundException if the fitness class does not exist
     * @throws InformationExistsException if the customer already booked the class
     * @throws IllegalArgumentException if the class is unavailable or full
     */
    @Transactional
    public BookingResponse createBooking(Long fitnessClassId) {

        logger.info("Creating booking for fitness class {}", fitnessClassId);

        AuthenticatedUser currentUser = getCurrentCustomer();

        FitnessClass fitnessClass =
                fitnessClassRepository.findByIdForUpdate(fitnessClassId)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Fitness class with id "
                                                + fitnessClassId
                                                + " not found"
                                )
                        );

        if (fitnessClass.getStatus() != FitnessClass.Status.SCHEDULED) {
            throw new IllegalArgumentException(
                    "This fitness class is not available for booking"
            );
        }

        if (fitnessClass.getStartTime() == null
                || !fitnessClass.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "You cannot book a fitness class that has already started"
            );
        }

        boolean alreadyBooked =
                bookingRepository.existsByCustomerIdAndFitnessClassIdAndStatus(
                        currentUser.userId(),
                        fitnessClassId,
                        Booking.Status.CONFIRMED
                );

        if (alreadyBooked) {
            throw new InformationExistsException(
                    "You have already booked this fitness class"
            );
        }

        long confirmedBookings =
                bookingRepository.countByFitnessClassIdAndStatus(
                        fitnessClassId,
                        Booking.Status.CONFIRMED
                );

        long activeOffers =
                waitlistEntryService.countActiveOffers(fitnessClassId);

        if (fitnessClass.getCapacity() == null
                || fitnessClass.getCapacity() <= 0
                || confirmedBookings + activeOffers >= fitnessClass.getCapacity()) {
            throw new IllegalArgumentException(
                    "This fitness class is full or its available places are reserved."
            );
        }

        Booking booking = new Booking();
        booking.setCustomerId(currentUser.userId());
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);

        auditLogService.log(
                AuditLog.Action.BOOK_CLASS,
                "User " + currentUser.userId()
                        + " booked Fitness Class " + fitnessClassId,
                currentUser.userId()
        );

        eventPublisher.publishEvent(
                new CustomerNotificationEvent(
                        currentUser.userId(),
                        "Booking #" + savedBooking.getId()
                                + " confirmed for Fitness Class "
                                + fitnessClass.getName()
                )
        );

        eventPublisher.publishEvent(
                new BookingConfirmedEvent(
                        currentUser.userId(),
                        currentUser.email(),
                        savedBooking.getId(),
                        fitnessClass.getName(),
                        fitnessClass.getStartTime().toString(),
                        fitnessClass.getEndTime().toString()
                )
        );

        logger.info(
                "Booking created successfully with id {}",
                savedBooking.getId()
        );

        return new BookingResponse(savedBooking);
    }

    /**
     * Retrieves the authenticated customer's bookings in paginated form.
     *
     * @param pageable pagination and sorting information
     * @return a page of the customer's booking details
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(Pageable pageable) {

        AuthenticatedUser currentUser = getCurrentCustomer();

        logger.info(
                "Retrieving bookings for customer {}",
                currentUser.userId()
        );

        return bookingRepository.findByCustomerId(
                currentUser.userId(),
                pageable
        ).map(BookingResponse::new);
    }

    /**
     * Retrieves a booking after checking the current user's access permissions.
     *
     * @param id the ID of the booking
     * @return the booking details
     * @throws InformationNotFoundException if the booking does not exist
     * @throws ForbiddenException if the customer does not own the booking
     */
    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id) {

        logger.info("Retrieving booking with id {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Booking with id " + id + " not found"
                        )
                );

        AuthenticatedUser currentUser = getCurrentUser();

        if ("CUSTOMER".equalsIgnoreCase(currentUser.role())
                && !booking.getCustomerId().equals(currentUser.userId())) {
            throw new ForbiddenException(
                    "You are not allowed to view this booking"
            );
        }

        return new BookingResponse(booking);
    }

    /**
     * Retrieves bookings for a fitness class, subject to role-based access rules.
     *
     * @param fitnessClassId the ID of the fitness class
     * @return the list of booking details
     * @throws InformationNotFoundException if the fitness class does not exist
     * @throws ForbiddenException if the user cannot view the bookings
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByFitnessClass(Long fitnessClassId) {

        logger.info(
                "Retrieving bookings for fitness class {}",
                fitnessClassId
        );

        FitnessClass fitnessClass =
                fitnessClassRepository.findById(fitnessClassId)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Fitness class with id "
                                                + fitnessClassId
                                                + " not found"
                                )
                        );

        AuthenticatedUser currentUser = getCurrentUser();

        if ("TRAINER".equalsIgnoreCase(currentUser.role())
                && !fitnessClass.getTrainerId().equals(currentUser.userId())) {
            throw new ForbiddenException(
                    "You can only view bookings for your own fitness classes"
            );
        }

        if (!"ADMIN".equalsIgnoreCase(currentUser.role())
                && !"TRAINER".equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenException(
                    "You are not allowed to view bookings for this fitness class"
            );
        }

        return bookingRepository.findByFitnessClassId(fitnessClassId)
                .stream()
                .map(BookingResponse::new)
                .toList();
    }

    /**
     * Retrieves all bookings for administrative purposes.
     *
     * @return all booking details
     * @throws ForbiddenException if the current user is not an administrator
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {

        AuthenticatedUser currentUser = getCurrentUser();

        if (!"ADMIN".equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenException(
                    "Only administrators can retrieve all bookings"
            );
        }

        logger.info("Admin retrieving all bookings");

        return bookingRepository.findAll()
                .stream()
                .map(BookingResponse::new)
                .toList();
    }

    /**
     * Cancels a booking if cancellation is permitted by the booking rules.
     *
     * @param id the ID of the booking to cancel
     * @throws InformationNotFoundException if the booking does not exist
     * @throws ForbiddenException if the customer does not own the booking
     * @throws IllegalArgumentException if the booking cannot be cancelled
     */
    @Transactional
    public void cancelBooking(Long id) {

        logger.info("Cancelling booking with id {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Booking with id " + id + " not found"
                        )
                );

        AuthenticatedUser currentUser = getCurrentUser();

        if ("CUSTOMER".equalsIgnoreCase(currentUser.role())
                && !booking.getCustomerId().equals(currentUser.userId())) {
            throw new ForbiddenException(
                    "You are only allowed to cancel your own bookings"
            );
        }

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new IllegalArgumentException(
                    "Booking is already cancelled"
            );
        }

        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new IllegalArgumentException(
                    "Completed bookings cannot be cancelled"
            );
        }

        FitnessClass fitnessClass = booking.getFitnessClass();

        if (fitnessClass.getStartTime() == null
                || !fitnessClass.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Bookings cannot be cancelled after the class has started"
            );
        }

        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);

        waitlistEntryService.notifyNextWaitingCustomer(
                fitnessClass.getId()
        );

        auditLogService.log(
                AuditLog.Action.CANCEL_BOOKING,
                "User " + currentUser.userId()
                        + " cancelled Booking " + booking.getId(),
                currentUser.userId()
        );

        eventPublisher.publishEvent(
                new CustomerNotificationEvent(
                        booking.getCustomerId(),
                        "Booking #" + booking.getId()
                                + " cancelled for Fitness Class "
                                + fitnessClass.getName()
                )
        );

        logger.info(
                "Booking {} cancelled successfully",
                booking.getId()
        );
    }

    /**
     * Deletes a booking and records the deletion in the audit log.
     *
     * @param id the ID of the booking to delete
     * @throws InformationNotFoundException if the booking does not exist
     * @throws ForbiddenException if the current user is not an administrator
     */
    @Transactional
    public void deleteBooking(Long id) {

        AuthenticatedUser currentUser = getCurrentUser();

        if (!"ADMIN".equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenException(
                    "Only administrators can delete bookings"
            );
        }

        logger.info("Admin deleting booking with id {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Booking with id " + id + " not found"
                        )
                );

        bookingRepository.delete(booking);

        auditLogService.log(
                AuditLog.Action.DELETE_BOOKING,
                "Admin " + currentUser.userId()
                        + " deleted Booking " + id,
                currentUser.userId()
        );

        logger.info(
                "Booking {} deleted successfully by admin",
                id
        );
    }

    /**
     * Retrieves the authenticated user from the security context.
     *
     * @return the authenticated user identity
     * @throws IllegalStateException if the user is not authenticated
     */
    private AuthenticatedUser getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof AuthenticatedUser currentUser)) {
            throw new IllegalStateException(
                    "An authenticated user is required."
            );
        }

        return currentUser;
    }

    /**
     * Retrieves the authenticated customer from the security context.
     *
     * @return the authenticated customer identity
     * @throws IllegalStateException if the user is not authenticated
     *         or does not have the customer role
     */
    private AuthenticatedUser getCurrentCustomer() {

        AuthenticatedUser currentUser = getCurrentUser();

        if (!"CUSTOMER".equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenException(
                    "Only customers can perform this booking operation."
            );
        }

        return currentUser;
    }
}
