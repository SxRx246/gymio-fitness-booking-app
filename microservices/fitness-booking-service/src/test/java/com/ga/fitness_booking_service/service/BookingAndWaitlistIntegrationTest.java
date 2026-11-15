package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.model.WaitlistEntry;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.UserRepository;
import com.ga.gymio.repository.WaitlistEntryRepository;
import com.ga.gymio.dto.request.WaitlistEntryRequest;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "server.port=0",
                "jwt-secret=test-secret-for-concurrency-tests",
                "spring.mail.username=test@example.com",
                "spring.mail.password=test-password"
        }
)
class BookingAndWaitlistIntegrationTest {

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

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private NotificationService notificationService;

    private User trainer;
    private FitnessClass fitnessClass;
    private List<User> customers;

    @BeforeEach
    void setUp() {
        String uniqueId = UUID.randomUUID().toString();

        trainer = createUser(
                "trainer-" + uniqueId + "@example.com",
                User.Role.TRAINER
        );

        customers = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            customers.add(createUser(
                    "customer-" + i + "-" + uniqueId + "@example.com",
                    User.Role.CUSTOMER
            ));
        }

        fitnessClass = new FitnessClass();
        fitnessClass.setName("Booking and Waitlist Integration Test");
        fitnessClass.setDescription("Tests booking capacity and waitlisting");
        fitnessClass.setType(FitnessClass.Type.FULLBODY);
        fitnessClass.setLevel(FitnessClass.Level.BEGINNER);
        fitnessClass.setStartTime(LocalDateTime.now().plusDays(2));
        fitnessClass.setEndTime(
                LocalDateTime.now().plusDays(2).plusHours(1)
        );
        fitnessClass.setCapacity(2);
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setTrainer(trainer);

        fitnessClass = fitnessClassRepository.saveAndFlush(fitnessClass);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();

        if (fitnessClass != null && fitnessClass.getId() != null) {
            waitlistEntryRepository.deleteAll(
                    waitlistEntryRepository
                            .findAllByFitnessClass_IdAndStatusOrderByJoinedAtAsc(
                                    fitnessClass.getId(),
                                    WaitlistEntry.Status.WAITING
                            )
            );

            // Also remove any other waitlist statuses for this class.
            for (User customer : customers) {
                if (customer.getId() != null) {
                    waitlistEntryRepository
                            .findByCustomer_IdAndFitnessClass_Id(
                                    customer.getId(),
                                    fitnessClass.getId()
                            )
                            .ifPresent(waitlistEntryRepository::delete);
                }
            }

            bookingRepository.deleteAll(
                    bookingRepository.findByFitnessClassId(
                            fitnessClass.getId()
                    )
            );

            fitnessClassRepository.deleteById(fitnessClass.getId());
        }

        if (customers != null) {
            customers.stream()
                    .filter(user -> user.getId() != null)
                    .forEach(user -> userRepository.deleteById(user.getId()));
        }

        if (trainer != null && trainer.getId() != null) {
            userRepository.deleteById(trainer.getId());
        }
    }

    @Test
    void concurrentBookingAndWaitlistWorkflowShouldRespectClassCapacity()
            throws Exception {

        int numberOfCustomers = customers.size();

        ExecutorService executor =
                Executors.newFixedThreadPool(numberOfCustomers);

        CountDownLatch ready = new CountDownLatch(numberOfCustomers);
        CountDownLatch start = new CountDownLatch(1);

        List<User> unsuccessfulCustomers = new ArrayList<>();

        try {
            List<Future<Boolean>> results = new ArrayList<>();

            for (User customer : customers) {
                results.add(executor.submit(() -> {
                    MyUserDetails userDetails = new MyUserDetails(customer);

                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            )
                    );

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
                        SecurityContextHolder.clearContext();
                    }
                }));
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "All booking tasks should be ready before starting"
            );

            start.countDown();

            for (int i = 0; i < results.size(); i++) {
                boolean bookingSucceeded =
                        results.get(i).get(30, TimeUnit.SECONDS);

                if (!bookingSucceeded) {
                    unsuccessfulCustomers.add(customers.get(i));
                }
            }

            long confirmedBookings =
                    bookingRepository.countByFitnessClassIdAndStatus(
                            fitnessClass.getId(),
                            Booking.Status.CONFIRMED
                    );

            assertEquals(
                    2,
                    confirmedBookings,
                    "Exactly two bookings should be confirmed"
            );

            assertEquals(
                    3,
                    unsuccessfulCustomers.size(),
                    "Three customers should be unable to book"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Booking worker threads should terminate"
            );
        }

        // Joining the waitlist is an explicit action by each unsuccessful
        // customer; a failed booking does not add them automatically.
        for (User customer : unsuccessfulCustomers) {
            MyUserDetails userDetails = new MyUserDetails(customer);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    )
            );

            try {
                WaitlistEntryRequest request = new WaitlistEntryRequest();
                request.setFitnessClassId(fitnessClass.getId());

                var response = waitlistEntryService.joinWaitlist(request);

                assertNotNull(response.getId());
                assertEquals(
                        fitnessClass.getId(),
                        response.getFitnessClassId()
                );
                assertEquals(
                        WaitlistEntry.Status.WAITING,
                        response.getStatus()
                );

            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        long confirmedBookings =
                bookingRepository.countByFitnessClassIdAndStatus(
                        fitnessClass.getId(),
                        Booking.Status.CONFIRMED
                );

        List<WaitlistEntry> waitingEntries =
                waitlistEntryRepository
                        .findAllByFitnessClass_IdAndStatusOrderByJoinedAtAsc(
                                fitnessClass.getId(),
                                WaitlistEntry.Status.WAITING
                        );

        assertEquals(2, confirmedBookings);
        assertEquals(
                3,
                waitingEntries.size(),
                "All three unsuccessful customers should be waiting"
        );
    }

    private User createUser(String email, User.Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("test-password");
        user.setRole(role);
        user.setStatus(User.Status.ACTIVE);
        user.setEmailVerified(true);

        return userRepository.saveAndFlush(user);
    }
}