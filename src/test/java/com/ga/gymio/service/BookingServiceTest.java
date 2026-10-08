package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.exception.ForbiddenException;
import com.ga.gymio.exception.InformationExistsException;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.service.AuditLogService;
import com.ga.gymio.service.BookingService;
import com.ga.gymio.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FitnessClassRepository fitnessClassRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BookingService bookingService;

    private User customer;
    private User anotherCustomer;
    private FitnessClass fitnessClass;

    @BeforeEach
    void setUp() {

        customer = new User();
        customer.setId(1L);
        customer.setEmail("customer@gymio.com");
        customer.setRole(User.Role.CUSTOMER);
        customer.setStatus(User.Status.ACTIVE);
        customer.setEmailVerified(true);

        anotherCustomer = new User();
        anotherCustomer.setId(2L);
        anotherCustomer.setEmail("another@gymio.com");
        anotherCustomer.setRole(User.Role.CUSTOMER);
        anotherCustomer.setStatus(User.Status.ACTIVE);
        anotherCustomer.setEmailVerified(true);

        fitnessClass = new FitnessClass();
        fitnessClass.setId(10L);
        fitnessClass.setName("Morning Full Body");
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setStartTime(LocalDateTime.now().plusDays(1));
        fitnessClass.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        fitnessClass.setCapacity(10);

        setAuthenticatedUser(customer);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(User user) {

        MyUserDetails userDetails =
                new MyUserDetails(user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @Test
    void createBooking_shouldCreateBookingSuccessfully() {

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository
                .existsByCustomerIdAndFitnessClassId(1L, 10L))
                .thenReturn(false);

        when(bookingRepository
                .countByFitnessClassIdAndStatus(
                        10L,
                        Booking.Status.CONFIRMED))
                .thenReturn(2L);

        Booking savedBooking = new Booking();
        savedBooking.setId(100L);
        savedBooking.setCustomer(customer);
        savedBooking.setFitnessClass(fitnessClass);
        savedBooking.setStatus(Booking.Status.CONFIRMED);

        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(savedBooking);

        var response = bookingService.createBooking(10L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(Booking.Status.CONFIRMED, response.getStatus());

        verify(bookingRepository).save(any(Booking.class));

        verify(emailService).sendBookingConfirmationEmail(
                eq("customer@gymio.com"),
                eq("Morning Full Body"),
                anyString(),
                anyString()
        );
    }

    @Test
    void createBooking_shouldRejectDuplicateBooking() {

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository
                .existsByCustomerIdAndFitnessClassId(1L, 10L))
                .thenReturn(true);

        assertThrows(
                InformationExistsException.class,
                () -> bookingService.createBooking(10L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void createBooking_shouldRejectFullClass() {

        fitnessClass.setCapacity(2);

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository
                .existsByCustomerIdAndFitnessClassId(1L, 10L))
                .thenReturn(false);

        when(bookingRepository
                .countByFitnessClassIdAndStatus(
                        10L,
                        Booking.Status.CONFIRMED))
                .thenReturn(2L);

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(10L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void createBooking_shouldRejectStartedClass() {

        fitnessClass.setStartTime(
                LocalDateTime.now().minusMinutes(10)
        );

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(10L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void createBooking_shouldRejectNonScheduledClass() {

        fitnessClass.setStatus(
                FitnessClass.Status.CANCELLED
        );

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(10L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void cancelBooking_shouldCancelOwnBooking() {

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setCustomer(customer);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        when(bookingRepository.findById(100L))
                .thenReturn(Optional.of(booking));

        bookingService.cancelBooking(100L);

        assertEquals(
                Booking.Status.CANCELLED,
                booking.getStatus()
        );

        verify(bookingRepository).save(booking);

        verify(emailService).sendBookingCancellationEmail(
                eq("customer@gymio.com"),
                eq("Morning Full Body"),
                anyString(),
                anyString()
        );
    }

    @Test
    void cancelBooking_shouldRejectOtherCustomersBooking() {

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setCustomer(anotherCustomer);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        when(bookingRepository.findById(100L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.cancelBooking(100L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void cancelBooking_shouldRejectCompletedBooking() {

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setCustomer(customer);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.COMPLETED);

        when(bookingRepository.findById(100L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(100L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void cancelBooking_shouldRejectAlreadyCancelledBooking() {

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setCustomer(customer);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CANCELLED);

        when(bookingRepository.findById(100L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(100L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void getBooking_shouldRejectCustomerViewingAnotherCustomersBooking() {

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setCustomer(anotherCustomer);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        when(bookingRepository.findById(100L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.getBooking(100L)
        );
    }
}