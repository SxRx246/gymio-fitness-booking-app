package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.dto.response.BookingResponse;
import com.ga.gymio.exception.ForbiddenException;
import com.ga.gymio.exception.InformationExistsException;
import com.ga.gymio.exception.InformationNotFoundException;
import com.ga.gymio.model.AuditLog;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.FitnessClassRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final WaitlistEntryService waitlistEntryService;

    private static final Logger logger =
            LoggerFactory.getLogger(BookingService.class);

    /**
     * Creates a booking if the fitness class is available and has capacity.
     *
     * @param fitnessClassId the ID of the fitness class to book
     * @return the created booking details
     */
    @Transactional
    public BookingResponse createBooking(Long fitnessClassId) {

        logger.info(
                "Creating booking for fitness class {}",
                fitnessClassId
        );

        User currentUser = getCurrentUser();

        FitnessClass fitnessClass =
                fitnessClassRepository.findByIdForUpdate(fitnessClassId)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Fitness class with id "
                                                + fitnessClassId
                                                + " not found"
                                )
                        );

        try {
            Thread.sleep(6000); // TEMPORARY: simulate processing delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Booking interrupted", e);
        }

        if (fitnessClass.getStatus()
                != FitnessClass.Status.SCHEDULED) {

            throw new IllegalArgumentException(
                    "This fitness class is not available for booking"
            );
        }

        if (!fitnessClass.getStartTime()
                .isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "You cannot book a fitness class that has already started"
            );
        }

        boolean alreadyBooked =
                bookingRepository.existsByCustomerIdAndFitnessClassIdAndStatus(
                        currentUser.getId(),
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

        if (confirmedBookings + activeOffers >= fitnessClass.getCapacity()) {
            throw new IllegalArgumentException(
                    "This fitness class is full or its available places are reserved."
            );
        }

        Booking booking = new Booking();

        booking.setCustomer(currentUser);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);

        auditLogService.log(
                AuditLog.Action.BOOK_CLASS,
                "User " + currentUser.getId() + " booked Fitness Class " + fitnessClass.getId(),
                currentUser);

        notificationService.sendNotification(
                currentUser.getId(),
                "Booking #" + savedBooking.getId()
                        + " confirmed for Fitness Class "
                        + fitnessClass.getName()
        );

        emailService.sendBookingConfirmationEmail(
                currentUser.getEmail(),
                fitnessClass.getName(),
                fitnessClass.getStartTime().toString(),
                fitnessClass.getEndTime().toString()
        );

        logger.info(
                "Booking created successfully with id {}",
                savedBooking.getId()
        );

        return new BookingResponse(savedBooking);
    }

    /**
     * Retrieves the current customer's bookings in paginated form.
     *
     * @param pageable pagination and sorting information
     * @return a page of the customer's booking details
     */
    public Page<BookingResponse> getMyBookings(Pageable pageable) {

        User currentUser = getCurrentUser();

        logger.info(
                "Retrieving bookings for customer {}",
                currentUser.getId()
        );

        return bookingRepository.findByCustomerId(
                currentUser.getId(),
                pageable
        ).map(BookingResponse::new);
    }

    /**
     * Retrieves a booking after checking the current user's access permissions.
     *
     * @param id the ID of the booking
     * @return the booking details
     */
    public BookingResponse getBooking(Long id) {

        logger.info(
                "Retrieving booking with id {}",
                id
        );

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Booking with id " + id + " not found"
                        )
                );

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.CUSTOMER
                && !booking.getCustomer().getId()
                .equals(currentUser.getId())) {

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
     */
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

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.TRAINER
                && !fitnessClass.getTrainer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You can only view bookings for your own fitness classes");
        }

        if (currentUser.getRole() != User.Role.ADMIN
                && currentUser.getRole() != User.Role.TRAINER) {
            throw new ForbiddenException("You are not allowed to view bookings for this fitness class");
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
     */
    public List<BookingResponse> getAllBookings() {

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
     */
    @Transactional
    public void cancelBooking(Long id) {

        logger.info(
                "Cancelling booking with id {}",
                id
        );

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Booking with id " + id + " not found"
                        )
                );

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.CUSTOMER
                && !booking.getCustomer().getId()
                .equals(currentUser.getId())) {

            throw new ForbiddenException(
                    "You are only allowed to cancel your own bookings"
            );
        }

        if (booking.getStatus()
                == Booking.Status.CANCELLED) {

            throw new IllegalArgumentException(
                    "Booking is already cancelled"
            );
        }

        if (booking.getStatus()
                == Booking.Status.COMPLETED) {

            throw new IllegalArgumentException(
                    "Completed bookings cannot be cancelled"
            );
        }

        FitnessClass fitnessClass =
                booking.getFitnessClass();

        if (!fitnessClass.getStartTime()
                .isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Bookings cannot be cancelled after the class has started"
            );
        }

        booking.setStatus(Booking.Status.CANCELLED);

        bookingRepository.save(booking);

        waitlistEntryService.notifyNextWaitingCustomer(
                booking.getFitnessClass().getId()
        );

        auditLogService.log(
                AuditLog.Action.CANCEL_BOOKING,
                "User " + currentUser.getId() + " cancelled Booking " +
                        booking.getId(), currentUser
        );

        notificationService.sendNotification(
                booking.getCustomer().getId(),
                "Booking #" + booking.getId()
                        + " cancelled for Fitness Class "
                        + fitnessClass.getName()
        );

        emailService.sendBookingCancellationEmail(
                booking.getCustomer().getEmail(),
                fitnessClass.getName(),
                fitnessClass.getStartTime().toString(),
                fitnessClass.getEndTime().toString()
        );

        logger.info(
                "Booking {} cancelled successfully",
                booking.getId()
        );
    }

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        MyUserDetails myUserDetails =
                (MyUserDetails) authentication.getPrincipal();

        return myUserDetails.getUser();
    }

    /**
     * Deletes a booking and records the deletion in the audit log.
     *
     * @param id the ID of the booking to delete
     */
    public void deleteBooking(Long id) {

        logger.info("Admin deleting booking with id {}", id);

        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Booking with id " + id + " not found"
                        )
                );

        bookingRepository.delete(booking);

        auditLogService.log(
                AuditLog.Action.DELETE_BOOKING,
                "Admin " + currentUser.getId() + " deleted Booking " + id,
                currentUser
        );

        logger.info(
                "Booking {} deleted successfully by admin",
                id
        );
    }
}