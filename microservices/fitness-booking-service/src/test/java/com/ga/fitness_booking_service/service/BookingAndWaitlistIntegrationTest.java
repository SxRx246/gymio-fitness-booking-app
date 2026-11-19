package com.ga.fitness_booking_service.service;

import com.ga.fitness_booking_service.dto.AuthenticatedUser;
import com.ga.fitness_booking_service.dto.request.WaitlistEntryRequest;
import com.ga.fitness_booking_service.exception.InformationNotFoundException;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "server.port=0",
                "jwt.secret=test-secret-for-concurrency-tests",
                "app.internal-auth.secret=test-internal-secret"
        }
)
class BookingAndWaitlistIntegrationTest {

    private static final AtomicLong CUSTOMER_ID_GENERATOR =
            new AtomicLong(900_000_000L);

    @Autowired
    private BookingService bookingService;

    @Autowired
    private WaitlistEntryService waitlistEntryService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private FitnessClassRepository fitnessClassRepository;

    @MockitoBean
    private AuditLogService auditLogService;

    private FitnessClass fitnessClass;

    private List<Long> customerIds;

    /**
     * Creates a future fitness class and customer IDs before each test.
     */
    @BeforeEach
    void setUp() {
        customerIds = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            customerIds.add(CUSTOMER_ID_GENERATOR.incrementAndGet());
        }

        fitnessClass = createFitnessClass(2);
    }

    /**
     * Removes all bookings, waitlist entries, and the test fitness class.
     */
    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();

        if (fitnessClass == null || fitnessClass.getId() == null) {
            return;
        }

        Long classId = fitnessClass.getId();

        List<WaitlistEntry> entries = waitlistEntryRepository.findAll()
                .stream()
                .filter(entry ->
                        entry.getFitnessClass().getId().equals(classId))
                .toList();

        waitlistEntryRepository.deleteAll(entries);

        bookingRepository.deleteAll(
                bookingRepository.findByFitnessClassId(classId)
        );

        fitnessClassRepository.deleteById(classId);
    }

    /**
     * Verifies that a customer can successfully book an available class.
     */
    @Test
    void customerCanBookAvailableClass() {
        Long customerId = customerIds.get(0);

        authenticate(customerId);

        try {
            assertDoesNotThrow(
                    () -> bookingService.createBooking(fitnessClass.getId())
            );
        } finally {
            clearAuthentication();
        }

        assertTrue(
                bookingRepository.existsByCustomerIdAndFitnessClassIdAndStatus(
                        customerId,
                        fitnessClass.getId(),
                        Booking.Status.CONFIRMED
                )
        );
    }

    /**
     * Verifies that a customer cannot book the same class twice.
     */
    @Test
    void customerCannotBookSameClassTwice() {
        Long customerId = customerIds.get(0);

        authenticate(customerId);

        try {
            bookingService.createBooking(fitnessClass.getId());

            assertThrows(
                    RuntimeException.class,
                    () -> bookingService.createBooking(fitnessClass.getId())
            );
        } finally {
            clearAuthentication();
        }

        assertEquals(
                1,
                bookingRepository.countByFitnessClassIdAndStatus(
                        fitnessClass.getId(),
                        Booking.Status.CONFIRMED
                )
        );
    }

    /**
     * Verifies that a booking attempt for a nonexistent class is rejected.
     */
    @Test
    void bookingNonexistentClassShouldFail() {
        authenticate(customerIds.get(0));

        try {
            assertThrows(
                    InformationNotFoundException.class,
                    () -> bookingService.createBooking(-999L)
            );
        } finally {
            clearAuthentication();
        }
    }

    /**
     * Verifies that a class with an invalid status cannot be booked.
     */
    @Test
    void customerCannotBookCancelledClass() {
        fitnessClass.setStatus(FitnessClass.Status.CANCELLED);
        fitnessClassRepository.saveAndFlush(fitnessClass);

        authenticate(customerIds.get(0));

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> bookingService.createBooking(fitnessClass.getId())
            );
        } finally {
            clearAuthentication();
        }
    }

    /**
     * Verifies that a class that has already started cannot be booked.
     */
    @Test
    void customerCannotBookStartedClass() {
        fitnessClass.setStartTime(LocalDateTime.now().minusHours(1));
        fitnessClass.setEndTime(LocalDateTime.now().plusHours(1));
        fitnessClassRepository.saveAndFlush(fitnessClass);

        authenticate(customerIds.get(0));

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> bookingService.createBooking(fitnessClass.getId())
            );
        } finally {
            clearAuthentication();
        }
    }

    /**
     * Verifies that customers cannot exceed the class capacity.
     */
    @Test
    void bookingShouldRespectClassCapacity() {
        fitnessClass.setCapacity(1);
        fitnessClassRepository.saveAndFlush(fitnessClass);

        authenticate(customerIds.get(0));

        try {
            bookingService.createBooking(fitnessClass.getId());
        } finally {
            clearAuthentication();
        }

        authenticate(customerIds.get(1));

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> bookingService.createBooking(fitnessClass.getId())
            );
        } finally {
            clearAuthentication();
        }

        assertEquals(
                1,
                bookingRepository.countByFitnessClassIdAndStatus(
                        fitnessClass.getId(),
                        Booking.Status.CONFIRMED
                )
        );
    }

    /**
     * Verifies that only customers can create bookings.
     */
    @Test
    void nonCustomerCannotBookClass() {
        authenticateWithRole(customerIds.get(0), "TRAINER");

        try {
            assertThrows(
                    RuntimeException.class,
                    () -> bookingService.createBooking(fitnessClass.getId())
            );
        } finally {
            clearAuthentication();
        }
    }

    /**
     * Verifies that an unauthenticated user cannot create a booking.
     */
    @Test
    void unauthenticatedUserCannotBookClass() {
        clearAuthentication();

        assertThrows(
                RuntimeException.class,
                () -> bookingService.createBooking(fitnessClass.getId())
        );
    }

    /**
     * Verifies that customers can join a full class waitlist.
     */
    @Test
    void customerCanJoinFullClassWaitlist() {
        fillClassToCapacity();

        Long customerId = customerIds.get(2);

        authenticate(customerId);

        try {
            waitlistEntryService.joinWaitlist(createWaitlistRequest());
        } finally {
            clearAuthentication();
        }

        assertEquals(
                1,
                countEntries(WaitlistEntry.Status.WAITING)
        );
    }

    /**
     * Verifies that customers cannot join a waitlist when a place is available.
     */
    @Test
    void customerCannotJoinWaitlistWhenPlaceIsAvailable() {
        authenticate(customerIds.get(0));

        try {
            assertThrows(
                    IllegalStateException.class,
                    () -> waitlistEntryService.joinWaitlist(
                            createWaitlistRequest()
                    )
            );
        } finally {
            clearAuthentication();
        }
    }

    /**
     * Verifies that the same customer cannot join the same waitlist twice.
     */
    @Test
    void customerCannotJoinSameWaitlistTwice() {
        fillClassToCapacity();

        Long customerId = customerIds.get(2);

        authenticate(customerId);

        try {
            waitlistEntryService.joinWaitlist(createWaitlistRequest());

            assertThrows(
                    IllegalStateException.class,
                    () -> waitlistEntryService.joinWaitlist(
                            createWaitlistRequest()
                    )
            );
        } finally {
            clearAuthentication();
        }

        assertEquals(1, countEntries(WaitlistEntry.Status.WAITING));
    }

    /**
     * Verifies that a customer can reactivate a previously cancelled entry.
     */
    @Test
    void customerCanRejoinAfterLeavingWaitlist() {
        fillClassToCapacity();

        Long customerId = customerIds.get(2);

        authenticate(customerId);

        try {
            waitlistEntryService.joinWaitlist(createWaitlistRequest());
        } finally {
            clearAuthentication();
        }

        WaitlistEntry entry = findEntry(customerId);

        entry.setStatus(WaitlistEntry.Status.CANCELLED);
        waitlistEntryRepository.saveAndFlush(entry);

        authenticate(customerId);

        try {
            waitlistEntryService.joinWaitlist(createWaitlistRequest());
        } finally {
            clearAuthentication();
        }

        assertEquals(
                1,
                waitlistEntryRepository.findAllByFitnessClass_IdAndStatusOrderByJoinedAtAsc(
                        fitnessClass.getId(),
                        WaitlistEntry.Status.WAITING
                ).size()
        );
    }

    /**
     * Verifies that a customer can retrieve their own waitlist entries.
     */
    @Test
    void customerCanRetrieveOwnWaitlistEntries() {
        fillClassToCapacity();

        Long customerId = customerIds.get(2);

        authenticate(customerId);

        try {
            waitlistEntryService.joinWaitlist(createWaitlistRequest());

            assertEquals(
                    1,
                    waitlistEntryService.getMyWaitlist().size()
            );
        } finally {
            clearAuthentication();
        }
    }

    /**
     * Verifies that a customer can leave a waiting entry.
     */
    @Test
    void customerCanLeaveWaitlist() {
        fillClassToCapacity();

        Long customerId = customerIds.get(2);

        authenticate(customerId);

        try {
            waitlistEntryService.joinWaitlist(createWaitlistRequest());
        } finally {
            clearAuthentication();
        }

        WaitlistEntry entry = findEntry(customerId);

        authenticate(customerId);

        try {
            waitlistEntryService.leaveWaitlist(entry.getId());
        } finally {
            clearAuthentication();
        }

        assertEquals(
                WaitlistEntry.Status.CANCELLED,
                waitlistEntryRepository.findById(entry.getId())
                        .orElseThrow()
                        .getStatus()
        );
    }

    /**
     * Verifies that a customer cannot leave another customer's waitlist entry.
     */
    @Test
    void customerCannotLeaveAnotherCustomersEntry() {
        fillClassToCapacity();

        Long ownerId = customerIds.get(2);

        authenticate(ownerId);

        try {
            waitlistEntryService.joinWaitlist(createWaitlistRequest());
        } finally {
            clearAuthentication();
        }

        WaitlistEntry entry = findEntry(ownerId);

        authenticate(customerIds.get(3));

        try {
            assertThrows(
                    InformationNotFoundException.class,
                    () -> waitlistEntryService.leaveWaitlist(entry.getId())
            );
        } finally {
            clearAuthentication();
        }

        assertEquals(
                WaitlistEntry.Status.WAITING,
                waitlistEntryRepository.findById(entry.getId())
                        .orElseThrow()
                        .getStatus()
        );
    }

    /**
     * Verifies that the next waiting customer receives an offer when a place
     * becomes available.
     */
    @Test
    void nextWaitingCustomerReceivesOfferWhenPlaceBecomesAvailable() {
        fitnessClass.setCapacity(1);
        fitnessClassRepository.saveAndFlush(fitnessClass);

        authenticate(customerIds.get(0));

        try {
            bookingService.createBooking(fitnessClass.getId());
        } finally {
            clearAuthentication();
        }

        Long waitingCustomerId = customerIds.get(2);

        authenticate(waitingCustomerId);

        try {
            waitlistEntryService.joinWaitlist(createWaitlistRequest());
        } finally {
            clearAuthentication();
        }

        bookingRepository.deleteAll(
                bookingRepository.findByFitnessClassId(fitnessClass.getId())
        );

        waitlistEntryService.notifyNextWaitingCustomer(fitnessClass.getId());

        WaitlistEntry entry = findEntry(waitingCustomerId);

        assertEquals(
                WaitlistEntry.Status.NOTIFIED,
                entry.getStatus()
        );

        assertNotNull(entry.getOfferExpiresAt());
        assertTrue(entry.getOfferExpiresAt().isAfter(LocalDateTime.now()));
    }

    /**
     * Verifies that a customer can decline an active offer.
     */
    @Test
    void customerCanDeclineOffer() {
        WaitlistEntry entry = createWaitlistEntry(
                customerIds.get(2),
                WaitlistEntry.Status.NOTIFIED,
                LocalDateTime.now().plusMinutes(30)
        );

        authenticate(customerIds.get(2));

        try {
            waitlistEntryService.declineOffer(entry.getId());
        } finally {
            clearAuthentication();
        }

        WaitlistEntry updated = waitlistEntryRepository
                .findById(entry.getId())
                .orElseThrow();

        assertEquals(WaitlistEntry.Status.CANCELLED, updated.getStatus());
        assertNull(updated.getOfferExpiresAt());
    }

    /**
     * Verifies that accepting an active offer creates a confirmed booking
     * and promotes the waitlist entry.
     */
    @Test
    void customerCanAcceptOfferAndGetBooking() {
        WaitlistEntry entry = createWaitlistEntry(
                customerIds.get(2),
                WaitlistEntry.Status.NOTIFIED,
                LocalDateTime.now().plusMinutes(30)
        );

        authenticate(customerIds.get(2));

        try {
            waitlistEntryService.acceptOffer(entry.getId());
        } finally {
            clearAuthentication();
        }

        WaitlistEntry updated = waitlistEntryRepository
                .findById(entry.getId())
                .orElseThrow();

        assertEquals(WaitlistEntry.Status.PROMOTED, updated.getStatus());

        assertTrue(
                bookingRepository.existsByCustomerIdAndFitnessClassIdAndStatus(
                        customerIds.get(2),
                        fitnessClass.getId(),
                        Booking.Status.CONFIRMED
                )
        );
    }

    /**
     * Verifies that a customer cannot accept an offer belonging to another
     * customer.
     */
    @Test
    void customerCannotAcceptAnotherCustomersOffer() {
        WaitlistEntry entry = createWaitlistEntry(
                customerIds.get(2),
                WaitlistEntry.Status.NOTIFIED,
                LocalDateTime.now().plusMinutes(30)
        );

        authenticate(customerIds.get(3));

        try {
            assertThrows(
                    InformationNotFoundException.class,
                    () -> waitlistEntryService.acceptOffer(entry.getId())
            );
        } finally {
            clearAuthentication();
        }

        assertEquals(
                WaitlistEntry.Status.NOTIFIED,
                waitlistEntryRepository.findById(entry.getId())
                        .orElseThrow()
                        .getStatus()
        );
    }

    /**
     * Verifies that an expired offer cannot be accepted.
     *
     * This test also checks that the expired entry is cancelled.
     * It exposes the transaction rollback issue in acceptOffer().
     */
    @Test
    void expiredOfferShouldBeCancelledWhenAcceptanceIsAttempted() {
        WaitlistEntry entry = createWaitlistEntry(
                customerIds.get(2),
                WaitlistEntry.Status.NOTIFIED,
                LocalDateTime.now().minusMinutes(5)
        );

        authenticate(customerIds.get(2));

        try {
            assertThrows(
                    IllegalStateException.class,
                    () -> waitlistEntryService.acceptOffer(entry.getId())
            );
        } finally {
            clearAuthentication();
        }

        assertEquals(
                WaitlistEntry.Status.CANCELLED,
                waitlistEntryRepository.findById(entry.getId())
                        .orElseThrow()
                        .getStatus()
        );
    }

    /**
     * Verifies that the scheduled expiration operation cancels expired offers.
     */
    @Test
    void expiredOffersAreCancelledByExpiryOperation() {
        WaitlistEntry entry = createWaitlistEntry(
                customerIds.get(2),
                WaitlistEntry.Status.NOTIFIED,
                LocalDateTime.now().minusMinutes(5)
        );

        waitlistEntryService.expireOffersAndNotifyNext();

        assertEquals(
                WaitlistEntry.Status.CANCELLED,
                waitlistEntryRepository.findById(entry.getId())
                        .orElseThrow()
                        .getStatus()
        );
    }

    /**
     * Verifies that a non-customer cannot manage waitlist entries.
     */
    @Test
    void nonCustomerCannotJoinWaitlist() {
        fillClassToCapacity();

        authenticateWithRole(customerIds.get(2), "TRAINER");

        try {
            assertThrows(
                    IllegalStateException.class,
                    () -> waitlistEntryService.joinWaitlist(
                            createWaitlistRequest()
                    )
            );
        } finally {
            clearAuthentication();
        }
    }

    /**
     * Verifies that concurrent booking attempts never exceed class capacity.
     *
     * @throws Exception if a concurrent task fails unexpectedly
     */
    @Test
    void concurrentBookingAndWaitlistWorkflowShouldRespectClassCapacity()
            throws Exception {

        int numberOfCustomers = customerIds.size();

        ExecutorService executor =
                Executors.newFixedThreadPool(numberOfCustomers);

        CountDownLatch ready = new CountDownLatch(numberOfCustomers);
        CountDownLatch start = new CountDownLatch(1);

        List<Long> unsuccessfulCustomers = new ArrayList<>();

        try {
            List<Future<Boolean>> results = new ArrayList<>();

            for (Long customerId : customerIds) {
                results.add(executor.submit(() -> {
                    authenticate(customerId);
                    ready.countDown();

                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "Timed out waiting to start booking attempts"
                            );
                        }

                        bookingService.createBooking(fitnessClass.getId());
                        return true;

                    } catch (IllegalArgumentException exception) {
                        return false;

                    } finally {
                        clearAuthentication();
                    }
                }));
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "All booking tasks should be ready before starting"
            );

            start.countDown();

            for (int i = 0; i < results.size(); i++) {
                boolean succeeded =
                        results.get(i).get(30, TimeUnit.SECONDS);

                if (!succeeded) {
                    unsuccessfulCustomers.add(customerIds.get(i));
                }
            }

        } finally {
            start.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Booking worker threads should terminate"
            );
        }

        assertEquals(
                2,
                bookingRepository.countByFitnessClassIdAndStatus(
                        fitnessClass.getId(),
                        Booking.Status.CONFIRMED
                )
        );

        assertEquals(4, unsuccessfulCustomers.size());

        for (Long customerId : unsuccessfulCustomers) {
            authenticate(customerId);

            try {
                waitlistEntryService.joinWaitlist(createWaitlistRequest());
            } finally {
                clearAuthentication();
            }
        }

        assertEquals(
                4,
                countEntries(WaitlistEntry.Status.WAITING)
        );
    }

    /**
     * Creates and persists a future scheduled fitness class.
     *
     * @param capacity the maximum number of bookings allowed
     * @return the saved fitness class
     */
    private FitnessClass createFitnessClass(int capacity) {
        LocalDateTime startTime = LocalDateTime.now().plusDays(2);

        FitnessClass newClass = new FitnessClass();
        newClass.setName("Booking and Waitlist Integration Test");
        newClass.setDescription("Integration test fitness class");
        newClass.setType(FitnessClass.Type.FULLBODY);
        newClass.setLevel(FitnessClass.Level.BEGINNER);
        newClass.setStartTime(startTime);
        newClass.setEndTime(startTime.plusHours(1));
        newClass.setCapacity(capacity);
        newClass.setStatus(FitnessClass.Status.SCHEDULED);
        newClass.setTrainerId(CUSTOMER_ID_GENERATOR.incrementAndGet());

        return fitnessClassRepository.saveAndFlush(newClass);
    }

    /**
     * Creates a waitlist request for the current test fitness class.
     *
     * @return the waitlist request
     */
    private WaitlistEntryRequest createWaitlistRequest() {
        WaitlistEntryRequest request = new WaitlistEntryRequest();
        request.setFitnessClassId(fitnessClass.getId());
        return request;
    }

    /**
     * Creates a confirmed booking to fill the class to capacity.
     */
    private void fillClassToCapacity() {
        for (int i = 0; i < fitnessClass.getCapacity(); i++) {
            authenticate(customerIds.get(i));

            try {
                bookingService.createBooking(fitnessClass.getId());
            } finally {
                clearAuthentication();
            }
        }
    }

    /**
     * Creates and persists a waitlist entry with the specified status.
     *
     * @param customerId the customer ID
     * @param status the waitlist status
     * @param expiration the offer expiration time
     * @return the saved waitlist entry
     */
    private WaitlistEntry createWaitlistEntry(
            Long customerId,
            WaitlistEntry.Status status,
            LocalDateTime expiration
    ) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setCustomerId(customerId);
        entry.setFitnessClass(fitnessClass);
        entry.setStatus(status);
        entry.setNotifiedAt(LocalDateTime.now());
        entry.setOfferExpiresAt(expiration);

        return waitlistEntryRepository.saveAndFlush(entry);
    }

    /**
     * Finds a customer's waitlist entry for the current test class.
     *
     * @param customerId the customer ID
     * @return the matching waitlist entry
     */
    private WaitlistEntry findEntry(Long customerId) {
        return waitlistEntryRepository
                .findByCustomerIdAndFitnessClass_Id(
                        customerId,
                        fitnessClass.getId()
                )
                .orElseThrow();
    }

    /**
     * Counts entries for the current test class with the specified status.
     *
     * @param status the waitlist status
     * @return the number of matching entries
     */
    private long countEntries(WaitlistEntry.Status status) {
        return waitlistEntryRepository
                .findAllByFitnessClass_IdAndStatusOrderByJoinedAtAsc(
                        fitnessClass.getId(),
                        status
                )
                .size();
    }

    /**
     * Sets the authenticated customer in the current thread's security context.
     *
     * @param customerId the customer ID
     */
    private void authenticate(Long customerId) {
        authenticateWithRole(customerId, "CUSTOMER");
    }

    /**
     * Sets an authenticated user with the specified role.
     *
     * @param userId the user ID
     * @param role the user's role
     */
    private void authenticateWithRole(Long userId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                role,
                "user-" + userId + "@example.com"
        );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );

        SecurityContextHolder.setContext(context);
    }

    /**
     * Clears the security context for the current thread.
     */
    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
