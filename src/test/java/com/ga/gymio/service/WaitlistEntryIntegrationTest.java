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
import com.ga.gymio.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "server.port=0",
                "jwt-secret=test-secret-for-concurrency-tests",
                "spring.mail.username=test@example.com",
                "spring.mail.password=test-password"
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

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private AuditLogService auditLogService;

    private User trainer;
    private User waitingCustomer;
    private User bookedCustomer;
    private FitnessClass fitnessClass;

    @BeforeEach
    void setUp() {
        String uniqueId = UUID.randomUUID().toString();
        trainer = createUser(
                "waitlist-trainer-"+ uniqueId +"@example.com",
                User.Role.TRAINER
        );

        waitingCustomer = createUser(
                "waitlist-customer-" + uniqueId + "@example.com",
                User.Role.CUSTOMER
        );

        bookedCustomer = createUser(
                "waitlist-booked-" + uniqueId + "@example.com",
                User.Role.CUSTOMER
        );

        fitnessClass = new FitnessClass();
        fitnessClass.setName("Waitlist Integration Test");
        fitnessClass.setDescription("Test class for waitlist integration");
        fitnessClass.setType(FitnessClass.Type.FULLBODY);
        fitnessClass.setLevel(FitnessClass.Level.BEGINNER);
        fitnessClass.setStartTime(LocalDateTime.now().plusDays(1));
        fitnessClass.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        fitnessClass.setCapacity(1);
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setTrainer(trainer);

        fitnessClass = fitnessClassRepository.saveAndFlush(fitnessClass);

        // Fill the class so the customer must join the waitlist.
        Booking booking = new Booking();
        booking.setCustomer(bookedCustomer);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        bookingRepository.saveAndFlush(booking);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();

        if (fitnessClass != null && fitnessClass.getId() != null) {
            // Remove waitlist entries before deleting the class.
            for (User customer : List.of(waitingCustomer)) {
                if (customer != null && customer.getId() != null) {
                    waitlistEntryRepository.deleteAll(
                            waitlistEntryRepository
                                    .findAllByCustomer_IdOrderByJoinedAtDesc(
                                            customer.getId()
                                    )
                    );
                }
            }

            bookingRepository.deleteAll(
                    bookingRepository.findByFitnessClassId(
                            fitnessClass.getId()
                    )
            );

            fitnessClassRepository.delete(fitnessClass);
        }

        if (waitingCustomer != null) {
            userRepository.delete(waitingCustomer);
        }

        if (bookedCustomer != null) {
            userRepository.delete(bookedCustomer);
        }

        if (trainer != null) {
            userRepository.delete(trainer);
        }
    }

    @Test
    void joinWaitlist_WhenClassIsFull_SavesWaitingEntry() {
        // Authenticate as the customer joining the waitlist.
        MyUserDetails userDetails = new MyUserDetails(waitingCustomer);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );

        WaitlistEntryRequest request = new WaitlistEntryRequest();
        request.setFitnessClassId(fitnessClass.getId());

        WaitlistEntryResponse response =
                waitlistEntryService.joinWaitlist(request);

        assertNotNull(response.getId());
        assertEquals(fitnessClass.getId(), response.getFitnessClassId());
        assertEquals(WaitlistEntry.Status.WAITING, response.getStatus());

        var savedEntry =
                waitlistEntryRepository
                        .findByCustomer_IdAndFitnessClass_Id(
                                waitingCustomer.getId(),
                                fitnessClass.getId()
                        );

        assertTrue(savedEntry.isPresent());
        assertEquals(
                WaitlistEntry.Status.WAITING,
                savedEntry.get().getStatus()
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
