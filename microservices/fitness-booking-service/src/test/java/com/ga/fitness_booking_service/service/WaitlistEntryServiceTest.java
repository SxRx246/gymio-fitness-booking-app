package com.ga.fitness_booking_service.service;

import com.ga.fitness_booking_service.dto.AuthenticatedUser;
import com.ga.fitness_booking_service.dto.request.WaitlistEntryRequest;
import com.ga.fitness_booking_service.dto.response.WaitlistEntryResponse;
import com.ga.fitness_booking_service.model.Booking;
import com.ga.fitness_booking_service.model.FitnessClass;
import com.ga.fitness_booking_service.model.WaitlistEntry;
import com.ga.fitness_booking_service.repository.BookingRepository;
import com.ga.fitness_booking_service.repository.FitnessClassRepository;
import com.ga.fitness_booking_service.repository.WaitlistEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitlistEntryServiceTest {

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;

    @Mock
    private FitnessClassRepository fitnessClassRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WaitlistEntryService waitlistEntryService;

    private AuthenticatedUser customer;
    private FitnessClass fitnessClass;
    private WaitlistEntryRequest request;

    @BeforeEach
    void setUp() {

        customer = new AuthenticatedUser(
                10L,
                "CUSTOMER",
                "customer@example.com"
        );

        fitnessClass = new FitnessClass();
        fitnessClass.setId(20L);
        fitnessClass.setName("Test Fitness Class");
        fitnessClass.setCapacity(1);
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setStartTime(LocalDateTime.now().plusDays(1));
        fitnessClass.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        request = new WaitlistEntryRequest();
        request.setFitnessClassId(fitnessClass.getId());

        authenticateAs(customer);

        when(fitnessClassRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository.countByFitnessClassIdAndStatus(
                20L,
                Booking.Status.CONFIRMED
        )).thenReturn(1L);

        when(waitlistEntryRepository
                .countByFitnessClass_IdAndStatusAndOfferExpiresAtAfter(
                        eq(20L),
                        eq(WaitlistEntry.Status.NOTIFIED),
                        any(LocalDateTime.class)
                )).thenReturn(0L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Sets the authenticated user in the security context.
     *
     * @param user the authenticated user
     */
    private void authenticateAs(AuthenticatedUser user) {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    // --------------------------------------------------
    // JOIN WAITLIST
    // --------------------------------------------------

    @Test
    void joinWaitlist_WhenClassIsFull_SavesWaitingEntry() {

        when(waitlistEntryRepository
                .findByCustomerIdAndFitnessClass_Id(10L, 20L))
                .thenReturn(Optional.empty());

        when(waitlistEntryRepository.save(any(WaitlistEntry.class)))
                .thenAnswer(invocation -> {
                    WaitlistEntry entry = invocation.getArgument(0);
                    entry.setId(100L);
                    return entry;
                });

        WaitlistEntryResponse response =
                waitlistEntryService.joinWaitlist(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(20L, response.getFitnessClassId());
        assertEquals(
                WaitlistEntry.Status.WAITING,
                response.getStatus()
        );

        verify(waitlistEntryRepository)
                .save(any(WaitlistEntry.class));
    }

    @Test
    void joinWaitlist_WhenPlaceIsAvailable_ThrowsException() {

        fitnessClass.setCapacity(2);

        when(bookingRepository.countByFitnessClassIdAndStatus(
                20L,
                Booking.Status.CONFIRMED
        )).thenReturn(0L);

        assertThrows(
                IllegalStateException.class,
                () -> waitlistEntryService.joinWaitlist(request)
        );

        verify(waitlistEntryRepository, never())
                .save(any(WaitlistEntry.class));
    }

    @Test
    void joinWaitlist_WhenActiveEntryExists_ThrowsException() {

        WaitlistEntry existingEntry = new WaitlistEntry();
        existingEntry.setId(100L);
        existingEntry.setCustomerId(10L);
        existingEntry.setFitnessClass(fitnessClass);
        existingEntry.setStatus(WaitlistEntry.Status.WAITING);

        when(waitlistEntryRepository
                .findByCustomerIdAndFitnessClass_Id(10L, 20L))
                .thenReturn(Optional.of(existingEntry));

        assertThrows(
                IllegalStateException.class,
                () -> waitlistEntryService.joinWaitlist(request)
        );

        verify(waitlistEntryRepository, never())
                .save(any(WaitlistEntry.class));
    }

    @Test
    void joinWaitlist_WhenCancelledEntryExists_ReactivatesEntry() {

        WaitlistEntry existingEntry = new WaitlistEntry();
        existingEntry.setId(100L);
        existingEntry.setCustomerId(10L);
        existingEntry.setFitnessClass(fitnessClass);
        existingEntry.setStatus(WaitlistEntry.Status.CANCELLED);
        existingEntry.setNotifiedAt(LocalDateTime.now());
        existingEntry.setOfferExpiresAt(
                LocalDateTime.now().plusMinutes(30)
        );

        when(waitlistEntryRepository
                .findByCustomerIdAndFitnessClass_Id(10L, 20L))
                .thenReturn(Optional.of(existingEntry));

        when(waitlistEntryRepository.save(existingEntry))
                .thenReturn(existingEntry);

        WaitlistEntryResponse response =
                waitlistEntryService.joinWaitlist(request);

        assertEquals(
                WaitlistEntry.Status.WAITING,
                response.getStatus()
        );

        assertNull(existingEntry.getNotifiedAt());
        assertNull(existingEntry.getOfferExpiresAt());

        verify(waitlistEntryRepository).save(existingEntry);
    }
}
