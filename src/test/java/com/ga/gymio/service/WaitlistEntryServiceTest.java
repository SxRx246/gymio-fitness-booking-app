package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.dto.request.WaitlistEntryRequest;
import com.ga.gymio.dto.response.WaitlistEntryResponse;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.model.WaitlistEntry;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.WaitlistEntryRepository;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitlistEntryServiceTest {

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;

    @Mock
    private FitnessClassRepository fitnessClassRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WaitlistEntryService waitlistEntryService;

    private User customer;
    private FitnessClass fitnessClass;
    private WaitlistEntryRequest request;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(10L);
        customer.setRole(User.Role.CUSTOMER);
        customer.setEmail("customer@example.com");

        fitnessClass = new FitnessClass();
        fitnessClass.setId(20L);
        fitnessClass.setCapacity(1);
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setStartTime(LocalDateTime.now().plusDays(1));
        fitnessClass.setName("Test Fitness Class");

        request = new WaitlistEntryRequest();
        request.setFitnessClassId(fitnessClass.getId());

        MyUserDetails userDetails = new MyUserDetails(customer);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );

        when(fitnessClassRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository.countByFitnessClassIdAndStatus(
                20L,
                Booking.Status.CONFIRMED
        )).thenReturn(1L);

        when(waitlistEntryRepository
                .countByFitnessClass_IdAndStatusAndOfferExpiresAtAfter(
                        org.mockito.ArgumentMatchers.eq(20L),
                        org.mockito.ArgumentMatchers.eq(
                                WaitlistEntry.Status.NOTIFIED
                        ),
                        any(LocalDateTime.class)
                )).thenReturn(0L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void joinWaitlist_WhenClassIsFull_SavesWaitingEntry() {
        when(waitlistEntryRepository
                .findByCustomer_IdAndFitnessClass_Id(10L, 20L))
                .thenReturn(Optional.empty());

        when(waitlistEntryRepository.save(any(WaitlistEntry.class)))
                .thenAnswer(invocation -> {
                    WaitlistEntry entry = invocation.getArgument(0);
                    entry.setId(100L);
                    return entry;
                });

        WaitlistEntryResponse response =
                waitlistEntryService.joinWaitlist(request);

        assertNotNull(response.getId());
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
    }

    @Test
    void joinWaitlist_WhenActiveEntryExists_ThrowsException() {
        WaitlistEntry existingEntry = new WaitlistEntry();
        existingEntry.setId(100L);
        existingEntry.setCustomer(customer);
        existingEntry.setFitnessClass(fitnessClass);
        existingEntry.setStatus(WaitlistEntry.Status.WAITING);

        when(waitlistEntryRepository
                .findByCustomer_IdAndFitnessClass_Id(10L, 20L))
                .thenReturn(Optional.of(existingEntry));

        assertThrows(
                IllegalStateException.class,
                () -> waitlistEntryService.joinWaitlist(request)
        );
    }

    @Test
    void joinWaitlist_WhenCancelledEntryExists_ReactivatesEntry() {
        WaitlistEntry existingEntry = new WaitlistEntry();
        existingEntry.setId(100L);
        existingEntry.setCustomer(customer);
        existingEntry.setFitnessClass(fitnessClass);
        existingEntry.setStatus(WaitlistEntry.Status.CANCELLED);
        existingEntry.setNotifiedAt(LocalDateTime.now());
        existingEntry.setOfferExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(waitlistEntryRepository
                .findByCustomer_IdAndFitnessClass_Id(10L, 20L))
                .thenReturn(Optional.of(existingEntry));

        when(waitlistEntryRepository.save(existingEntry))
                .thenReturn(existingEntry);

        WaitlistEntryResponse response =
                waitlistEntryService.joinWaitlist(request);

        assertEquals(
                WaitlistEntry.Status.WAITING,
                response.getStatus()
        );
        assertEquals(null, existingEntry.getNotifiedAt());
        assertEquals(null, existingEntry.getOfferExpiresAt());

        verify(waitlistEntryRepository).save(existingEntry);
    }
}
