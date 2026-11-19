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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "server.port=0",
                "jwt.secret=test-secret-for-waitlist-tests",
                "app.internal-auth.secret=test-internal-secret"
        }
)
@ActiveProfiles("test")
class WaitlistEntryIntegrationTest {

    @Autowired
    private WaitlistEntryService waitlistEntryService;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private FitnessClassRepository fitnessClassRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private AuditLogService auditLogService;

    private final Long trainerId = 1001L;
    private final Long waitingCustomerId = 2001L;
    private final Long bookedCustomerId = 2002L;

    private FitnessClass fitnessClass;

    @BeforeEach
    void setUp() {

        fitnessClass = new FitnessClass();

        fitnessClass.setName("Waitlist Integration Test");
        fitnessClass.setDescription("Test class for waitlist integration");
        fitnessClass.setType(FitnessClass.Type.FULLBODY);
        fitnessClass.setLevel(FitnessClass.Level.BEGINNER);
        fitnessClass.setStartTime(LocalDateTime.now().plusDays(1));
        fitnessClass.setEndTime(
                LocalDateTime.now().plusDays(1).plusHours(1)
        );
        fitnessClass.setCapacity(1);
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setTrainerId(trainerId);

        fitnessClass = fitnessClassRepository.saveAndFlush(fitnessClass);

        // Fill the class so the customer must join the waitlist.
        Booking booking = new Booking();

        booking.setCustomerId(bookedCustomerId);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        bookingRepository.saveAndFlush(booking);
    }

    @AfterEach
    void cleanUp() {

        SecurityContextHolder.clearContext();

        if (fitnessClass != null && fitnessClass.getId() != null) {

            waitlistEntryRepository.deleteAll(
                    waitlistEntryRepository
                            .findAllByCustomerIdOrderByJoinedAtDesc(
                                    waitingCustomerId
                            )
            );

            bookingRepository.deleteAll(
                    bookingRepository.findByFitnessClassId(
                            fitnessClass.getId()
                    )
            );

            fitnessClassRepository.delete(fitnessClass);
        }
    }

    /**
     * Authenticates a customer for the current test.
     */
    private void authenticateAsCustomer() {

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                waitingCustomerId,
                "CUSTOMER",
                "waitlist-customer@example.com"
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @Test
    void joinWaitlist_WhenClassIsFull_SavesWaitingEntry() {

        authenticateAsCustomer();

        WaitlistEntryRequest request = new WaitlistEntryRequest();
        request.setFitnessClassId(fitnessClass.getId());

        WaitlistEntryResponse response =
                waitlistEntryService.joinWaitlist(request);

        assertNotNull(response);
        assertNotNull(response.getId());

        assertEquals(
                fitnessClass.getId(),
                response.getFitnessClassId()
        );

        assertEquals(
                WaitlistEntry.Status.WAITING,
                response.getStatus()
        );

        var savedEntry =
                waitlistEntryRepository
                        .findByCustomerIdAndFitnessClass_Id(
                                waitingCustomerId,
                                fitnessClass.getId()
                        );

        assertTrue(savedEntry.isPresent());

        assertEquals(
                WaitlistEntry.Status.WAITING,
                savedEntry.get().getStatus()
        );
    }
}
