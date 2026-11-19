package com.ga.fitness_booking_service.service;

import com.ga.fitness_booking_service.dto.AuthenticatedUser;
import com.ga.fitness_booking_service.dto.request.ClassReviewRequest;
import com.ga.fitness_booking_service.exception.ForbiddenException;
import com.ga.fitness_booking_service.exception.InformationNotFoundException;
import com.ga.fitness_booking_service.model.Booking;
import com.ga.fitness_booking_service.model.ClassReview;
import com.ga.fitness_booking_service.repository.BookingRepository;
import com.ga.fitness_booking_service.repository.ClassReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClassReviewService.
 *
 * Tests review creation, updating, deletion, retrieval,
 * ownership validation, and input validation.
 */
@ExtendWith(MockitoExtension.class)
class ClassReviewServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long OTHER_CUSTOMER_ID = 2L;
    private static final Long BOOKING_ID = 20L;
    private static final Long REVIEW_ID = 30L;
    private static final Long FITNESS_CLASS_ID = 10L;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ClassReviewRepository classReviewRepository;

    @InjectMocks
    private ClassReviewService classReviewService;

    private Booking booking;
    private ClassReview review;

    /**
     * Creates a completed booking and a review for test scenarios.
     */
    @BeforeEach
    void setUp() {
        authenticateAs(CUSTOMER_ID, "CUSTOMER");

        booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setCustomerId(CUSTOMER_ID);
        booking.setStatus(Booking.Status.COMPLETED);

        review = new ClassReview();
        review.setId(REVIEW_ID);
        review.setBooking(booking);
        review.setRating(5);
        review.setComment("Excellent class!");
    }

    /**
     * Clears the security context after each test.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Authenticates a user for the current test.
     *
     * @param userId the authenticated user's ID
     * @param role the authenticated user's role
     */
    private void authenticateAs(Long userId, String role) {
        AuthenticatedUser user =
                new AuthenticatedUser(userId, role, "customer@example.com");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Creates a valid review request.
     *
     * @param bookingId the booking ID
     * @param rating the review rating
     * @param comment the review comment
     * @return a valid review request
     */
    private ClassReviewRequest createRequest(
            Long bookingId,
            Integer rating,
            String comment
    ) {
        ClassReviewRequest request = new ClassReviewRequest();
        request.setBookingId(bookingId);
        request.setRating(rating);
        request.setComment(comment);
        return request;
    }

    // CREATE REVIEW TESTS

    @Test
    void createReviewShouldSaveReviewForCompletedBooking() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));
        when(classReviewRepository.existsByBooking_Id(BOOKING_ID))
                .thenReturn(false);
        when(classReviewRepository.save(any(ClassReview.class)))
                .thenReturn(review);

        var response = classReviewService.createReview(
                createRequest(BOOKING_ID, 5, "Excellent class!")
        );

        assertNotNull(response);

        ArgumentCaptor<ClassReview> captor =
                ArgumentCaptor.forClass(ClassReview.class);

        verify(classReviewRepository).save(captor.capture());

        assertEquals(booking, captor.getValue().getBooking());
        assertEquals(5, captor.getValue().getRating());
        assertEquals("Excellent class!", captor.getValue().getComment());
    }

    @Test
    void createReviewShouldThrowWhenBookingDoesNotExist() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> classReviewService.createReview(
                        createRequest(BOOKING_ID, 5, "Good class")
                )
        );

        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void createReviewShouldRejectAnotherCustomersBooking() {
        booking.setCustomerId(OTHER_CUSTOMER_ID);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> classReviewService.createReview(
                        createRequest(BOOKING_ID, 5, "Good class")
                )
        );

        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void createReviewShouldRejectBookingThatIsNotCompleted() {
        booking.setStatus(Booking.Status.CONFIRMED);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.createReview(
                        createRequest(BOOKING_ID, 5, "Good class")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void createReviewShouldRejectDuplicateReview() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));
        when(classReviewRepository.existsByBooking_Id(BOOKING_ID))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.createReview(
                        createRequest(BOOKING_ID, 5, "Good class")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void createReviewShouldRejectInvalidRating() {
        assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.createReview(
                        createRequest(BOOKING_ID, 6, "Good class")
                )
        );

        verifyNoInteractions(bookingRepository);
        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void createReviewShouldRejectNullRating() {
        assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.createReview(
                        createRequest(BOOKING_ID, null, "Good class")
                )
        );

        verifyNoInteractions(bookingRepository);
    }

    // UPDATE REVIEW TESTS

    @Test
    void updateReviewShouldUpdateOwnReview() {
        when(classReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(review));
        when(classReviewRepository.save(any(ClassReview.class)))
                .thenReturn(review);

        var response = classReviewService.updateReview(
                REVIEW_ID,
                createRequest(BOOKING_ID, 4, "Updated comment")
        );

        assertNotNull(response);
        assertEquals(4, review.getRating());
        assertEquals("Updated comment", review.getComment());

        verify(classReviewRepository).save(review);
    }

    @Test
    void updateReviewShouldThrowWhenReviewDoesNotExist() {
        when(classReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> classReviewService.updateReview(
                        REVIEW_ID,
                        createRequest(BOOKING_ID, 4, "Updated comment")
                )
        );

        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void updateReviewShouldRejectAnotherCustomersReview() {
        booking.setCustomerId(OTHER_CUSTOMER_ID);

        when(classReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(review));

        assertThrows(
                ForbiddenException.class,
                () -> classReviewService.updateReview(
                        REVIEW_ID,
                        createRequest(BOOKING_ID, 4, "Updated comment")
                )
        );

        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void updateReviewShouldRejectChangingBookingId() {
        when(classReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(review));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.updateReview(
                        REVIEW_ID,
                        createRequest(99L, 4, "Updated comment")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void updateReviewShouldRejectInvalidRating() {
        assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.updateReview(
                        REVIEW_ID,
                        createRequest(BOOKING_ID, 0, "Invalid rating")
                )
        );

        verifyNoInteractions(classReviewRepository);
    }

    // DELETE REVIEW TESTS

    @Test
    void deleteReviewShouldDeleteOwnReview() {
        when(classReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(review));

        assertDoesNotThrow(
                () -> classReviewService.deleteReview(REVIEW_ID)
        );

        verify(classReviewRepository).delete(review);
    }

    @Test
    void deleteReviewShouldThrowWhenReviewDoesNotExist() {
        when(classReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> classReviewService.deleteReview(REVIEW_ID)
        );

        verify(classReviewRepository, never()).delete(any());
    }

    @Test
    void deleteReviewShouldRejectAnotherCustomersReview() {
        booking.setCustomerId(OTHER_CUSTOMER_ID);

        when(classReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(review));

        assertThrows(
                ForbiddenException.class,
                () -> classReviewService.deleteReview(REVIEW_ID)
        );

        verify(classReviewRepository, never()).delete(any());
    }

    // GET REVIEW BY BOOKING TESTS

    @Test
    void getReviewByBookingShouldReturnOwnReview() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));
        when(classReviewRepository.findByBooking_Id(BOOKING_ID))
                .thenReturn(Optional.of(review));

        var response = classReviewService.getReviewByBooking(BOOKING_ID);

        assertNotNull(response);
    }

    @Test
    void getReviewByBookingShouldThrowWhenBookingDoesNotExist() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> classReviewService.getReviewByBooking(BOOKING_ID)
        );
    }

    @Test
    void getReviewByBookingShouldRejectAnotherCustomersBooking() {
        booking.setCustomerId(OTHER_CUSTOMER_ID);

        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> classReviewService.getReviewByBooking(BOOKING_ID)
        );

        verify(classReviewRepository, never())
                .findByBooking_Id(any());
    }

    @Test
    void getReviewByBookingShouldThrowWhenReviewDoesNotExist() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking));
        when(classReviewRepository.findByBooking_Id(BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                InformationNotFoundException.class,
                () -> classReviewService.getReviewByBooking(BOOKING_ID)
        );
    }

    // GET REVIEWS BY FITNESS CLASS TESTS

    @Test
    void getReviewsByFitnessClassShouldReturnReviews() {
        when(classReviewRepository
                .findAllByBooking_FitnessClass_IdOrderByCreatedAtDesc(
                        FITNESS_CLASS_ID
                ))
                .thenReturn(List.of(review));

        var responses =
                classReviewService.getReviewsByFitnessClass(FITNESS_CLASS_ID);

        assertEquals(1, responses.size());
    }

    @Test
    void getReviewsByFitnessClassShouldReturnEmptyListWhenNoReviewsExist() {
        when(classReviewRepository
                .findAllByBooking_FitnessClass_IdOrderByCreatedAtDesc(
                        FITNESS_CLASS_ID
                ))
                .thenReturn(List.of());

        var responses =
                classReviewService.getReviewsByFitnessClass(FITNESS_CLASS_ID);

        assertTrue(responses.isEmpty());
    }

    // AUTHENTICATION TESTS

    @Test
    void createReviewShouldRejectNonCustomerRole() {
        authenticateAs(CUSTOMER_ID, "TRAINER");

        assertThrows(
                ForbiddenException.class,
                () -> classReviewService.createReview(
                        createRequest(BOOKING_ID, 5, "Good class")
                )
        );

        verifyNoInteractions(bookingRepository, classReviewRepository);
    }

    @Test
    void createReviewShouldRejectUnauthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThrows(
                IllegalStateException.class,
                () -> classReviewService.createReview(
                        createRequest(BOOKING_ID, 5, "Good class")
                )
        );

        verifyNoInteractions(bookingRepository, classReviewRepository);
    }
}
