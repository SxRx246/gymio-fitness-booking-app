package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.dto.request.FitnessClassRequest;
import com.ga.gymio.exception.ForbiddenException;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.FitnessClassRepository;
import com.ga.gymio.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FitnessClassServiceTest {

    @Mock
    private FitnessClassRepository fitnessClassRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FitnessClassService fitnessClassService;

    private User trainer;
    private User anotherTrainer;
    private User admin;
    private FitnessClass fitnessClass;

    @BeforeEach
    void setUp() {

        trainer = new User();
        trainer.setId(1L);
        trainer.setEmail("trainer@gymio.com");
        trainer.setRole(User.Role.TRAINER);
        trainer.setStatus(User.Status.ACTIVE);
        trainer.setEmailVerified(true);

        anotherTrainer = new User();
        anotherTrainer.setId(2L);
        anotherTrainer.setEmail("trainer2@gymio.com");
        anotherTrainer.setRole(User.Role.TRAINER);
        anotherTrainer.setStatus(User.Status.ACTIVE);
        anotherTrainer.setEmailVerified(true);

        admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@gymio.com");
        admin.setRole(User.Role.ADMIN);
        admin.setStatus(User.Status.ACTIVE);
        admin.setEmailVerified(true);

        fitnessClass = new FitnessClass();
        fitnessClass.setId(10L);
        fitnessClass.setName("Morning Full Body");
        fitnessClass.setDescription("Full body workout");
        fitnessClass.setType(FitnessClass.Type.FULLBODY);
        fitnessClass.setLevel(FitnessClass.Level.BEGINNER);
        fitnessClass.setStartTime(LocalDateTime.now().plusDays(1));
        fitnessClass.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        fitnessClass.setCapacity(10);
        fitnessClass.setStatus(FitnessClass.Status.SCHEDULED);
        fitnessClass.setTrainer(trainer);

        setAuthenticatedUser(trainer);
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
    void validateTrainer_shouldAcceptActiveVerifiedTrainer() {

        assertDoesNotThrow(() ->
                fitnessClassService.validateTrainer(trainer)
        );
    }

    @Test
    void validateTrainer_shouldRejectNonTrainer() {

        trainer.setRole(User.Role.CUSTOMER);

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.validateTrainer(trainer)
        );
    }

    @Test
    void validateTrainer_shouldRejectInactiveTrainer() {

        trainer.setStatus(User.Status.INACTIVE);

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.validateTrainer(trainer)
        );
    }

    @Test
    void validateTrainer_shouldRejectUnverifiedTrainer() {

        trainer.setEmailVerified(false);

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.validateTrainer(trainer)
        );
    }

    @Test
    void createFitnessClass_shouldCreateClassSuccessfully() {

        FitnessClassRequest request = createValidRequest();

        when(userRepository.findByEmail("trainer@gymio.com"))
                .thenReturn(Optional.of(trainer));

        when(fitnessClassRepository.save(any(FitnessClass.class)))
                .thenAnswer(invocation -> {
                    FitnessClass saved =
                            invocation.getArgument(0);
                    saved.setId(20L);
                    return saved;
                });

        FitnessClass result =
                fitnessClassService.createFitnessClass(request);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals("Morning Full Body", result.getName());
        assertEquals(trainer, result.getTrainer());
        assertEquals(
                FitnessClass.Status.SCHEDULED,
                result.getStatus()
        );

        verify(fitnessClassRepository)
                .save(any(FitnessClass.class));

        verify(auditLogService).log(
                eq(com.ga.gymio.model.AuditLog.Action.CREATE_CLASS),
                anyString(),
                eq(trainer)
        );
    }

    @Test
    void createFitnessClass_shouldRejectTrainerCreatingClassForAnotherTrainer() {

        FitnessClassRequest request = createValidRequest();
        request.setTrainerEmail("trainer2@gymio.com");

        when(userRepository.findByEmail("trainer2@gymio.com"))
                .thenReturn(Optional.of(anotherTrainer));

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.createFitnessClass(request)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void createFitnessClass_shouldRejectInvalidTime() {

        FitnessClassRequest request = createValidRequest();

        request.setStartTime(
                LocalDateTime.now().plusDays(2)
        );

        request.setEndTime(
                LocalDateTime.now().plusDays(1)
        );

        when(userRepository.findByEmail("trainer@gymio.com"))
                .thenReturn(Optional.of(trainer));

        assertThrows(
                IllegalArgumentException.class,
                () -> fitnessClassService.createFitnessClass(request)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void updateFitnessClass_shouldRejectAnotherTrainer() {

        setAuthenticatedUser(anotherTrainer);

        FitnessClassRequest request = createValidRequest();

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.updateFitnessClass(
                        request,
                        10L
                )
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void updateFitnessClass_shouldRejectCompletedClass() {

        fitnessClass.setStatus(
                FitnessClass.Status.COMPLETED
        );

        FitnessClassRequest request = createValidRequest();

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                IllegalArgumentException.class,
                () -> fitnessClassService.updateFitnessClass(
                        request,
                        10L
                )
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void updateFitnessClass_shouldUpdateOwnClass() {

        FitnessClassRequest request = createValidRequest();

        request.setName("Updated Full Body");

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        when(fitnessClassRepository.save(any(FitnessClass.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        FitnessClass result =
                fitnessClassService.updateFitnessClass(
                        request,
                        10L
                );

        assertEquals(
                "Updated Full Body",
                result.getName()
        );

        verify(fitnessClassRepository)
                .save(fitnessClass);

        verify(auditLogService).log(
                eq(com.ga.gymio.model.AuditLog.Action.UPDATE_CLASS),
                anyString(),
                eq(trainer)
        );
    }

    @Test
    void cancelFitnessClass_shouldCancelClassAndConfirmedBookings() {

        User customer = new User();
        customer.setId(4L);
        customer.setEmail("customer@gymio.com");
        customer.setRole(User.Role.CUSTOMER);
        customer.setStatus(User.Status.ACTIVE);
        customer.setEmailVerified(true);

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setCustomer(customer);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository.findByFitnessClassIdAndStatus(
                10L,
                Booking.Status.CONFIRMED
        )).thenReturn(List.of(booking));

        when(fitnessClassRepository.save(any(FitnessClass.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        fitnessClassService.cancelFitnessClass(10L);

        assertEquals(
                FitnessClass.Status.CANCELLED,
                fitnessClass.getStatus()
        );

        assertEquals(
                Booking.Status.CANCELLED,
                booking.getStatus()
        );

        verify(bookingRepository)
                .saveAll(List.of(booking));

        verify(emailService)
                .sendFitnessClassCancellationEmail(
                        eq("customer@gymio.com"),
                        eq("Morning Full Body"),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void cancelFitnessClass_shouldRejectCompletedClass() {

        fitnessClass.setStatus(
                FitnessClass.Status.COMPLETED
        );

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                IllegalArgumentException.class,
                () -> fitnessClassService.cancelFitnessClass(10L)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void updateFitnessClassStatuses_shouldCompleteFinishedClass() {

        fitnessClass.setStatus(
                FitnessClass.Status.SCHEDULED
        );

        fitnessClass.setStartTime(
                LocalDateTime.now().minusHours(2)
        );

        fitnessClass.setEndTime(
                LocalDateTime.now().minusHours(1)
        );

        when(fitnessClassRepository
                .findByStatusAndStartTimeLessThanEqual(
                        eq(FitnessClass.Status.SCHEDULED),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(fitnessClass));

        when(bookingRepository.findByFitnessClassIdAndStatus(
                10L,
                Booking.Status.CONFIRMED
        )).thenReturn(List.of());

        fitnessClassService.updateFitnessClassStatuses();

        assertEquals(
                FitnessClass.Status.COMPLETED,
                fitnessClass.getStatus()
        );

        verify(fitnessClassRepository)
                .saveAll(List.of(fitnessClass));
    }

    @Test
    void updateFitnessClassStatuses_shouldMoveStartedClassToInProgress() {

        fitnessClass.setStatus(
                FitnessClass.Status.SCHEDULED
        );

        fitnessClass.setStartTime(
                LocalDateTime.now().minusMinutes(30)
        );

        fitnessClass.setEndTime(
                LocalDateTime.now().plusMinutes(30)
        );

        when(fitnessClassRepository
                .findByStatusAndStartTimeLessThanEqual(
                        eq(FitnessClass.Status.SCHEDULED),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(fitnessClass));

        when(fitnessClassRepository
                .findByStatusAndEndTimeLessThanEqual(
                        eq(FitnessClass.Status.IN_PROGRESS),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of());

        fitnessClassService.updateFitnessClassStatuses();

        assertEquals(
                FitnessClass.Status.IN_PROGRESS,
                fitnessClass.getStatus()
        );
    }

    private FitnessClassRequest createValidRequest() {

        FitnessClassRequest request =
                new FitnessClassRequest();

        request.setName("Morning Full Body");
        request.setDescription("Full body workout");
        request.setType(FitnessClass.Type.FULLBODY);
        request.setLevel(FitnessClass.Level.BEGINNER);
        request.setStartTime(
                LocalDateTime.now().plusDays(1)
        );
        request.setEndTime(
                LocalDateTime.now().plusDays(1).plusHours(1)
        );
        request.setCapacity(10);
        request.setTrainerEmail("trainer@gymio.com");

        return request;
    }
}