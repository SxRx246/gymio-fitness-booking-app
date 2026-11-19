package com.ga.fitness_booking_service.service;

import com.ga.fitness_booking_service.client.UserServiceClient;
import com.ga.fitness_booking_service.dto.AuthenticatedUser;
import com.ga.fitness_booking_service.dto.request.FitnessClassRequest;
import com.ga.fitness_booking_service.dto.response.TrainerValidationResponse;
import com.ga.fitness_booking_service.event.CustomerNotificationEvent;
import com.ga.fitness_booking_service.exception.ForbiddenException;
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
import org.springframework.data.domain.Pageable;
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
    private BookingRepository bookingRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FitnessClassService fitnessClassService;

    private AuthenticatedUser trainer;
    private AuthenticatedUser anotherTrainer;
    private AuthenticatedUser admin;

    private TrainerValidationResponse validTrainer;
    private TrainerValidationResponse anotherValidTrainer;

    private FitnessClass fitnessClass;

    @BeforeEach
    void setUp() {

        trainer = new AuthenticatedUser(
                1L, "TRAINER", "trainer@gymio.com"
        );

        anotherTrainer = new AuthenticatedUser(
                2L, "TRAINER", "trainer2@gymio.com"
        );

        admin = new AuthenticatedUser(
                3L, "ADMIN", "admin@gymio.com"
        );

        validTrainer = new TrainerValidationResponse(
                1L,
                "trainer@gymio.com",
                "TRAINER",
                "ACTIVE",
                true
        );

        anotherValidTrainer = new TrainerValidationResponse(
                2L,
                "trainer2@gymio.com",
                "TRAINER",
                "ACTIVE",
                true
        );

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
        fitnessClass.setTrainerId(1L);

        authenticateAs(trainer);
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

    /**
     * Creates a valid fitness class request.
     *
     * @return a valid fitness class request
     */
    private FitnessClassRequest createValidRequest() {

        FitnessClassRequest request = new FitnessClassRequest();

        request.setName("Morning Full Body");
        request.setDescription("Full body workout");
        request.setType(FitnessClass.Type.FULLBODY);
        request.setLevel(FitnessClass.Level.BEGINNER);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        request.setCapacity(10);
        request.setTrainerEmail("trainer@gymio.com");

        return request;
    }

    // --------------------------------------------------
    // CREATE FITNESS CLASS
    // --------------------------------------------------

    @Test
    void createFitnessClass_shouldCreateClassSuccessfully() {

        FitnessClassRequest request = createValidRequest();

        when(userServiceClient.validateTrainerByEmail("trainer@gymio.com"))
                .thenReturn(Optional.of(validTrainer));

        when(fitnessClassRepository.save(any(FitnessClass.class)))
                .thenAnswer(invocation -> {
                    FitnessClass saved = invocation.getArgument(0);
                    saved.setId(20L);
                    return saved;
                });

        FitnessClass result =
                fitnessClassService.createFitnessClass(request);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals("Morning Full Body", result.getName());
        assertEquals(1L, result.getTrainerId());
        assertEquals(FitnessClass.Status.SCHEDULED, result.getStatus());

        verify(userServiceClient)
                .validateTrainerByEmail("trainer@gymio.com");

        verify(fitnessClassRepository)
                .save(any(FitnessClass.class));

        verify(auditLogService).log(
                eq(AuditLog.Action.CREATE_CLASS),
                anyString(),
                eq(1L)
        );
    }

    @Test
    void createFitnessClass_shouldRejectTrainerCreatingClassForAnotherTrainer() {

        FitnessClassRequest request = createValidRequest();
        request.setTrainerEmail("trainer2@gymio.com");

        when(userServiceClient.validateTrainerByEmail("trainer2@gymio.com"))
                .thenReturn(Optional.of(anotherValidTrainer));

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.createFitnessClass(request)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void createFitnessClass_shouldRejectInvalidTrainerRole() {

        FitnessClassRequest request = createValidRequest();

        TrainerValidationResponse invalidTrainer =
                new TrainerValidationResponse(
                        1L,
                        "trainer@gymio.com",
                        "CUSTOMER",
                        "ACTIVE",
                        true
                );

        when(userServiceClient.validateTrainerByEmail("trainer@gymio.com"))
                .thenReturn(Optional.of(invalidTrainer));

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.createFitnessClass(request)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void createFitnessClass_shouldRejectInactiveTrainer() {

        FitnessClassRequest request = createValidRequest();

        TrainerValidationResponse inactiveTrainer =
                new TrainerValidationResponse(
                        1L,
                        "trainer@gymio.com",
                        "TRAINER",
                        "INACTIVE",
                        true
                );

        when(userServiceClient.validateTrainerByEmail("trainer@gymio.com"))
                .thenReturn(Optional.of(inactiveTrainer));

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.createFitnessClass(request)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void createFitnessClass_shouldRejectUnverifiedTrainer() {

        FitnessClassRequest request = createValidRequest();

        TrainerValidationResponse unverifiedTrainer =
                new TrainerValidationResponse(
                        1L,
                        "trainer@gymio.com",
                        "TRAINER",
                        "ACTIVE",
                        false
                );

        when(userServiceClient.validateTrainerByEmail("trainer@gymio.com"))
                .thenReturn(Optional.of(unverifiedTrainer));

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.createFitnessClass(request)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void createFitnessClass_shouldRejectTrainerNotFound() {

        FitnessClassRequest request = createValidRequest();

        when(userServiceClient.validateTrainerByEmail("trainer@gymio.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> fitnessClassService.createFitnessClass(request)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void createFitnessClass_shouldRejectInvalidTime() {

        FitnessClassRequest request = createValidRequest();

        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(1));

        when(userServiceClient.validateTrainerByEmail("trainer@gymio.com"))
                .thenReturn(Optional.of(validTrainer));

        assertThrows(
                IllegalArgumentException.class,
                () -> fitnessClassService.createFitnessClass(request)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void createFitnessClass_shouldRejectCustomerRole() {

        authenticateAs(new AuthenticatedUser(
                4L, "CUSTOMER", "customer@gymio.com"
        ));

        FitnessClassRequest request = createValidRequest();

        when(userServiceClient.validateTrainerByEmail("trainer@gymio.com"))
                .thenReturn(Optional.of(validTrainer));

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.createFitnessClass(request)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    // --------------------------------------------------
    // GET FITNESS CLASS
    // --------------------------------------------------

    @Test
    void getFitnessClass_shouldReturnClassSuccessfully() {

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        FitnessClass result =
                fitnessClassService.getFitnessClass(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Morning Full Body", result.getName());
    }

    @Test
    void getFitnessClass_shouldRejectMissingClass() {

        when(fitnessClassRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> fitnessClassService.getFitnessClass(99L)
        );
    }

    // --------------------------------------------------
    // GET AND SEARCH FITNESS CLASSES
    // --------------------------------------------------

    @Test
    void getFitnessClasses_shouldReturnTrainerClasses() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<FitnessClass> expectedPage =
                new PageImpl<>(List.of(fitnessClass));

        when(fitnessClassRepository.findByTrainerId(1L, pageable))
                .thenReturn(expectedPage);

        Page<FitnessClass> result =
                fitnessClassService.getFitnessClasses(1L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getId());
    }

    @Test
    void searchFitnessClasses_shouldReturnFilteredClasses() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<FitnessClass> expectedPage =
                new PageImpl<>(List.of(fitnessClass));

        when(fitnessClassRepository.findByFilters(
                FitnessClass.Type.FULLBODY,
                FitnessClass.Level.BEGINNER,
                FitnessClass.Status.SCHEDULED,
                pageable
        )).thenReturn(expectedPage);

        Page<FitnessClass> result =
                fitnessClassService.searchFitnessClasses(
                        FitnessClass.Type.FULLBODY,
                        FitnessClass.Level.BEGINNER,
                        FitnessClass.Status.SCHEDULED,
                        pageable
                );

        assertEquals(1, result.getTotalElements());

        verify(fitnessClassRepository).findByFilters(
                FitnessClass.Type.FULLBODY,
                FitnessClass.Level.BEGINNER,
                FitnessClass.Status.SCHEDULED,
                pageable
        );
    }

    // --------------------------------------------------
    // UPDATE FITNESS CLASS
    // --------------------------------------------------

    @Test
    void updateFitnessClass_shouldUpdateOwnClass() {

        FitnessClassRequest request = createValidRequest();
        request.setName("Updated Full Body");

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        when(fitnessClassRepository.save(any(FitnessClass.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingRepository.findByFitnessClassIdAndStatus(
                10L,
                Booking.Status.CONFIRMED
        )).thenReturn(List.of());

        FitnessClass result =
                fitnessClassService.updateFitnessClass(request, 10L);

        assertEquals("Updated Full Body", result.getName());

        verify(fitnessClassRepository)
                .save(fitnessClass);

        verify(auditLogService).log(
                eq(AuditLog.Action.UPDATE_CLASS),
                anyString(),
                eq(1L)
        );
    }

    @Test
    void updateFitnessClass_shouldRejectAnotherTrainer() {

        authenticateAs(anotherTrainer);

        FitnessClassRequest request = createValidRequest();

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.updateFitnessClass(request, 10L)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void updateFitnessClass_shouldRejectCompletedClass() {

        fitnessClass.setStatus(FitnessClass.Status.COMPLETED);

        FitnessClassRequest request = createValidRequest();

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                IllegalArgumentException.class,
                () -> fitnessClassService.updateFitnessClass(request, 10L)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    @Test
    void updateFitnessClass_shouldAllowAdminToChangeTrainer() {

        authenticateAs(admin);

        FitnessClassRequest request = createValidRequest();
        request.setTrainerEmail("trainer2@gymio.com");

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        when(userServiceClient.validateTrainerByEmail("trainer2@gymio.com"))
                .thenReturn(Optional.of(anotherValidTrainer));

        when(fitnessClassRepository.save(any(FitnessClass.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingRepository.findByFitnessClassIdAndStatus(
                10L,
                Booking.Status.CONFIRMED
        )).thenReturn(List.of());

        FitnessClass result =
                fitnessClassService.updateFitnessClass(request, 10L);

        assertEquals(2L, result.getTrainerId());

        verify(userServiceClient)
                .validateTrainerByEmail("trainer2@gymio.com");
    }

    // --------------------------------------------------
    // CANCEL FITNESS CLASS
    // --------------------------------------------------

    @Test
    void cancelFitnessClass_shouldCancelClassAndConfirmedBookings() {

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setCustomerId(4L);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        when(bookingRepository.findByFitnessClassIdAndStatus(
                10L,
                Booking.Status.CONFIRMED
        )).thenReturn(List.of(booking));

        when(fitnessClassRepository.save(any(FitnessClass.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        verify(eventPublisher)
                .publishEvent(any(CustomerNotificationEvent.class));

        verify(auditLogService).log(
                eq(AuditLog.Action.CANCEL_CLASS),
                anyString(),
                eq(1L)
        );
    }

    @Test
    void cancelFitnessClass_shouldRejectCompletedClass() {

        fitnessClass.setStatus(FitnessClass.Status.COMPLETED);

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
    void cancelFitnessClass_shouldRejectAnotherTrainer() {

        authenticateAs(anotherTrainer);

        when(fitnessClassRepository.findById(10L))
                .thenReturn(Optional.of(fitnessClass));

        assertThrows(
                ForbiddenException.class,
                () -> fitnessClassService.cancelFitnessClass(10L)
        );

        verify(fitnessClassRepository, never())
                .save(any(FitnessClass.class));
    }

    // --------------------------------------------------
    // AUTOMATIC FITNESS CLASS STATUS UPDATES
    // --------------------------------------------------

    @Test
    void updateFitnessClassStatuses_shouldCompleteFinishedClass() {

        fitnessClass.setStartTime(LocalDateTime.now().minusHours(2));
        fitnessClass.setEndTime(LocalDateTime.now().minusHours(1));

        when(fitnessClassRepository.findByStatusAndStartTimeLessThanEqual(
                eq(FitnessClass.Status.SCHEDULED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(fitnessClass));

        when(fitnessClassRepository.findByStatusAndEndTimeLessThanEqual(
                eq(FitnessClass.Status.IN_PROGRESS),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

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

        fitnessClass.setStartTime(LocalDateTime.now().minusMinutes(30));
        fitnessClass.setEndTime(LocalDateTime.now().plusMinutes(30));

        when(fitnessClassRepository.findByStatusAndStartTimeLessThanEqual(
                eq(FitnessClass.Status.SCHEDULED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(fitnessClass));

        when(fitnessClassRepository.findByStatusAndEndTimeLessThanEqual(
                eq(FitnessClass.Status.IN_PROGRESS),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        fitnessClassService.updateFitnessClassStatuses();

        assertEquals(
                FitnessClass.Status.IN_PROGRESS,
                fitnessClass.getStatus()
        );

        verify(fitnessClassRepository)
                .saveAll(List.of(fitnessClass));
    }

    @Test
    void updateFitnessClassStatuses_shouldCompleteInProgressClass() {

        fitnessClass.setStatus(FitnessClass.Status.IN_PROGRESS);
        fitnessClass.setStartTime(LocalDateTime.now().minusHours(2));
        fitnessClass.setEndTime(LocalDateTime.now().minusHours(1));

        when(fitnessClassRepository.findByStatusAndStartTimeLessThanEqual(
                eq(FitnessClass.Status.SCHEDULED),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        when(fitnessClassRepository.findByStatusAndEndTimeLessThanEqual(
                eq(FitnessClass.Status.IN_PROGRESS),
                any(LocalDateTime.class)
        )).thenReturn(List.of(fitnessClass));

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
}
