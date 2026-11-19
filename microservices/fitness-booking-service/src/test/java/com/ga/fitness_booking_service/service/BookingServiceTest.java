package com.ga.fitness_booking_service.service;

import com.ga.fitness_booking_service.dto.AuthenticatedUser;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FitnessClassRepository fitnessClassRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private WaitlistEntryService waitlistEntryService;

    @InjectMocks
    private BookingService bookingService;

    private FitnessClass fitnessClass;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long ANOTHER_CUSTOMER_ID = 2L;
    private static final Long CLASS_ID = 10L;
    private static final Long BOOKING_ID = 100L;

    /**
     * Initializes the fitness class and authenticated customer.
     */
    @BeforeEach
    void setUp() {
        fitnessClass = new FitnessClass();
        fitnessClass.setId(CLASS_ID);
        fitnessClass.setName("Morning Full Body");
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setStartTime(LocalDateTime.now().plusDays(1));
        fitnessClass.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        fitnessClass.setCapacity(10);
        fitnessClass.setTrainerId(50L);

        authenticate(CUSTOMER_ID, "CUSTOMER");
    }

    /**
     * Clears the security context after each test.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Sets an authenticated user in the Spring Security context.
     *
     * @param userId the authenticated user's ID
     * @param role the authenticated user's role
     */
    private void authenticate(Long userId, String role) {
        AuthenticatedUser user =
                new AuthenticatedUser(userId, role, "customer@gymio.com");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    /**
     * Creates a booking associated with the test fitness class.
     *
     * @param customerId the customer's ID
     * @param status the booking status
     * @return the created booking object
     */
    private Booking createBooking(Long customerId, Booking.Status status) {
        Booking booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setCustomerId(customerId);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(status);
        return booking;
    }

    /**
     * Tests successful booking creation.
     */
    @Test
    void createBooking_shouldCreateBookingSuccessfully() {
        when(fitnessClassRepository.findByIdForUpdate(CLASS_ID))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository
                .existsByCustomerIdAndFitnessClassIdAndStatus(
                        CUSTOMER_ID, CLASS_ID, Booking.Status.CONFIRMED))
                .thenReturn(false);

        when(bookingRepository.countByFitnessClassIdAndStatus(
                CLASS_ID, Booking.Status.CONFIRMED))
                .thenReturn(2L);

        when(waitlistEntryService.countActiveOffers(CLASS_ID))
                .thenReturn(0L);

        Booking savedBooking =
                createBooking(CUSTOMER_ID, Booking.Status.CONFIRMED);

        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(savedBooking);

        var response = bookingService.createBooking(CLASS_ID);

        assertNotNull(response);
        assertEquals(BOOKING_ID, response.getId());
        assertEquals(Booking.Status.CONFIRMED, response.getStatus());

        verify(bookingRepository).save(any(Booking.class));
        verify(auditLogService).log(
                eq(AuditLog.Action.BOOK_CLASS),
                any(String.class),
                eq(CUSTOMER_ID)
        );

        verify(eventPublisher).publishEvent(
                any(CustomerNotificationEvent.class)
        );

        verify(eventPublisher).publishEvent(
                any(BookingConfirmedEvent.class)
        );
    }

    /**
     * Tests rejection of duplicate bookings.
     */
    @Test
    void createBooking_shouldRejectDuplicateBooking() {
        when(fitnessClassRepository.findByIdForUpdate(CLASS_ID))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository
                .existsByCustomerIdAndFitnessClassIdAndStatus(
                        CUSTOMER_ID, CLASS_ID, Booking.Status.CONFIRMED))
                .thenReturn(true);

        assertThrows(
                InformationExistsException.class,
                () -> bookingService.createBooking(CLASS_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when the fitness class does not exist.
     */
    @Test
    void createBooking_shouldRejectMissingFitnessClass() {
        when(fitnessClassRepository.findByIdForUpdate(CLASS_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> bookingService.createBooking(CLASS_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when the fitness class is full.
     */
    @Test
    void createBooking_shouldRejectFullClass() {
        fitnessClass.setCapacity(2);

        when(fitnessClassRepository.findByIdForUpdate(CLASS_ID))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository
                .existsByCustomerIdAndFitnessClassIdAndStatus(
                        CUSTOMER_ID, CLASS_ID, Booking.Status.CONFIRMED))
                .thenReturn(false);

        when(bookingRepository.countByFitnessClassIdAndStatus(
                CLASS_ID, Booking.Status.CONFIRMED))
                .thenReturn(2L);

        when(waitlistEntryService.countActiveOffers(CLASS_ID))
                .thenReturn(0L);

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(CLASS_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when all remaining places are reserved by active offers.
     */
    @Test
    void createBooking_shouldRejectWhenPlacesAreReserved() {
        fitnessClass.setCapacity(3);

        when(fitnessClassRepository.findByIdForUpdate(CLASS_ID))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository
                .existsByCustomerIdAndFitnessClassIdAndStatus(
                        CUSTOMER_ID, CLASS_ID, Booking.Status.CONFIRMED))
                .thenReturn(false);

        when(bookingRepository.countByFitnessClassIdAndStatus(
                CLASS_ID, Booking.Status.CONFIRMED))
                .thenReturn(2L);

        when(waitlistEntryService.countActiveOffers(CLASS_ID))
                .thenReturn(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(CLASS_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when the fitness class has already started.
     */
    @Test
    void createBooking_shouldRejectStartedClass() {
        fitnessClass.setStartTime(LocalDateTime.now().minusMinutes(10));

        when(fitnessClassRepository.findByIdForUpdate(CLASS_ID))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(CLASS_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when the fitness class is not scheduled.
     */
    @Test
    void createBooking_shouldRejectNonScheduledClass() {
        fitnessClass.setStatus(FitnessClass.Status.CANCELLED);

        when(fitnessClassRepository.findByIdForUpdate(CLASS_ID))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(CLASS_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when a non-customer attempts to create a booking.
     */
    @Test
    void createBooking_shouldRejectNonCustomer() {
        authenticate(50L, "TRAINER");

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.createBooking(CLASS_ID)
        );

        verify(fitnessClassRepository, never()).findByIdForUpdate(any());
    }

    /**
     * Tests rejection when no authenticated user exists.
     */
    @Test
    void createBooking_shouldRejectUnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(CLASS_ID)
        );
    }

    /**
     * Tests successful retrieval of the customer's bookings.
     */
    @Test
    void getMyBookings_shouldReturnCustomerBookings() {
        Booking booking =
                createBooking(CUSTOMER_ID, Booking.Status.CONFIRMED);

        PageRequest pageable = PageRequest.of(0, 10);

        when(bookingRepository.findByCustomerId(CUSTOMER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(booking)));

        Page<?> response = bookingService.getMyBookings(pageable);

        assertEquals(1, response.getTotalElements());

        verify(bookingRepository).findByCustomerId(CUSTOMER_ID, pageable);
    }

    /**
     * Tests successful retrieval of an owned booking.
     */
    @Test
    void getBooking_shouldReturnOwnBooking() {
        Booking booking =
                createBooking(CUSTOMER_ID, Booking.Status.CONFIRMED);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        var response = bookingService.getBooking(BOOKING_ID);

        assertNotNull(response);
        assertEquals(BOOKING_ID, response.getId());
    }

    /**
     * Tests rejection when a customer tries to view another customer's booking.
     */
    @Test
    void getBooking_shouldRejectAnotherCustomersBooking() {
        Booking booking =
                createBooking(ANOTHER_CUSTOMER_ID, Booking.Status.CONFIRMED);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.getBooking(BOOKING_ID)
        );
    }

    /**
     * Tests rejection when the requested booking does not exist.
     */
    @Test
    void getBooking_shouldRejectMissingBooking() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> bookingService.getBooking(BOOKING_ID)
        );
    }

    /**
     * Tests successful cancellation of the customer's booking.
     */
    @Test
    void cancelBooking_shouldCancelOwnBooking() {
        Booking booking =
                createBooking(CUSTOMER_ID, Booking.Status.CONFIRMED);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        bookingService.cancelBooking(BOOKING_ID);

        assertEquals(Booking.Status.CANCELLED, booking.getStatus());

        verify(bookingRepository).save(booking);
        verify(waitlistEntryService)
                .notifyNextWaitingCustomer(CLASS_ID);

        verify(eventPublisher).publishEvent(
                any(CustomerNotificationEvent.class)
        );
    }

    /**
     * Tests rejection when a customer tries to cancel another customer's booking.
     */
    @Test
    void cancelBooking_shouldRejectAnotherCustomersBooking() {
        Booking booking =
                createBooking(ANOTHER_CUSTOMER_ID, Booking.Status.CONFIRMED);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.cancelBooking(BOOKING_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when cancelling a completed booking.
     */
    @Test
    void cancelBooking_shouldRejectCompletedBooking() {
        Booking booking =
                createBooking(CUSTOMER_ID, Booking.Status.COMPLETED);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(BOOKING_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when cancelling an already cancelled booking.
     */
    @Test
    void cancelBooking_shouldRejectAlreadyCancelledBooking() {
        Booking booking =
                createBooking(CUSTOMER_ID, Booking.Status.CANCELLED);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(BOOKING_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when cancelling after the fitness class has started.
     */
    @Test
    void cancelBooking_shouldRejectStartedClass() {
        fitnessClass.setStartTime(LocalDateTime.now().minusMinutes(10));

        Booking booking =
                createBooking(CUSTOMER_ID, Booking.Status.CONFIRMED);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(BOOKING_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests rejection when the requested booking does not exist during cancellation.
     */
    @Test
    void cancelBooking_shouldRejectMissingBooking() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> bookingService.cancelBooking(BOOKING_ID)
        );

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    /**
     * Tests that only administrators can retrieve all bookings.
     */
    @Test
    void getAllBookings_shouldRejectNonAdmin() {
        assertThrows(
                ForbiddenException.class,
                () -> bookingService.getAllBookings()
        );

        verify(bookingRepository, never()).findAll();
    }

    /**
     * Tests that only administrators can delete bookings.
     */
    @Test
    void deleteBooking_shouldRejectNonAdmin() {
        assertThrows(
                ForbiddenException.class,
                () -> bookingService.deleteBooking(BOOKING_ID)
        );

        verify(bookingRepository, never()).delete(any(Booking.class));
    }
}
