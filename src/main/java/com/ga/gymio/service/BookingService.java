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

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    private static final Logger logger =
            LoggerFactory.getLogger(BookingService.class);

    @Transactional
    public BookingResponse createBooking(Long fitnessClassId) {

        logger.info(
                "Creating booking for fitness class {}",
                fitnessClassId
        );

        User currentUser = getCurrentUser();

        FitnessClass fitnessClass =
                fitnessClassRepository.findById(fitnessClassId)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Fitness class with id "
                                                + fitnessClassId
                                                + " not found"
                                )
                        );

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
                bookingRepository
                        .existsByCustomerIdAndFitnessClassId(
                                currentUser.getId(),
                                fitnessClassId
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

        if (confirmedBookings >= fitnessClass.getCapacity()) {
            throw new IllegalArgumentException(
                    "This fitness class is fully booked"
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
                currentUser );

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
                    "You can only view bookings for your own fitness classes" );
        }

        if (currentUser.getRole() != User.Role.ADMIN
                && currentUser.getRole() != User.Role.TRAINER) {
            throw new ForbiddenException( "You are not allowed to view bookings for this fitness class" );
        }

        return bookingRepository.findByFitnessClassId(fitnessClassId)
                .stream()
                .map(BookingResponse::new)
                .toList();
    }

    public List<BookingResponse> getAllBookings() {

        logger.info("Admin retrieving all bookings");

        return bookingRepository.findAll()
                .stream()
                .map(BookingResponse::new)
                .toList();
    }

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

        auditLogService.log(
                AuditLog.Action.CANCEL_BOOKING,
       "User " + currentUser.getId() + " cancelled Booking " +
                booking.getId(), currentUser
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
       "Admin " + currentUser.getId() + " deleted Booking " + id ,
                currentUser
        );

        logger.info(
                "Booking {} deleted successfully by admin",
                id
        );
    }
}