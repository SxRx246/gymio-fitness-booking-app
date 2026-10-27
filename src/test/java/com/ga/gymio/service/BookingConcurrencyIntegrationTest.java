package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.UserRepository;

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
class BookingConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

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

    @MockitoBean
    private WaitlistEntryService waitlistEntryService;

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
        fitnessClass.setName("Concurrency Test Class");
        fitnessClass.setDescription("Class used to test simultaneous bookings");
        fitnessClass.setType(FitnessClass.Type.FULLBODY);
        fitnessClass.setLevel(FitnessClass.Level.BEGINNER);
        fitnessClass.setStartTime(LocalDateTime.now().plusDays(2));
        fitnessClass.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        fitnessClass.setCapacity(2);
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setTrainer(trainer);

        fitnessClass = fitnessClassRepository.saveAndFlush(fitnessClass);

        when(waitlistEntryService.countActiveOffers(fitnessClass.getId()))
                .thenReturn(0L);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();

        if (fitnessClass != null && fitnessClass.getId() != null) {
            List<Booking> bookings =
                    bookingRepository.findByFitnessClassId(fitnessClass.getId());

            bookingRepository.deleteAll(bookings);
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
    void concurrentBookingsShouldNeverExceedClassCapacity() throws Exception {
        int numberOfCustomers = customers.size();

        ExecutorService executor =
                Executors.newFixedThreadPool(numberOfCustomers);

        CountDownLatch ready = new CountDownLatch(numberOfCustomers);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Boolean>> results = new ArrayList<>();

            for (User customer : customers) {
                results.add(executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    new MyUserDetails(customer),
                                    null,
                                    new MyUserDetails(customer).getAuthorities()
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
                        // The class is full; this booking should be rejected.
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

            // Release all workers as close together as possible.
            start.countDown();

            int successfulBookings = 0;

            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    successfulBookings++;
                }
            }

            long confirmedBookings =
                    bookingRepository.countByFitnessClassIdAndStatus(
                            fitnessClass.getId(),
                            Booking.Status.CONFIRMED
                    );

            assertEquals(
                    fitnessClass.getCapacity(),
                    successfulBookings,
                    "Only two customers should book the class successfully"
            );

            assertEquals(
                    fitnessClass.getCapacity().longValue(),
                    confirmedBookings,
                    "The database must not contain more confirmed bookings than capacity"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Booking worker threads should terminate"
            );
        }
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
