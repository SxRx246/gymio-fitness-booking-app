package com.ga.gymio.service;

import com.ga.gymio.dto.request.ClassReviewRequest;
import com.ga.gymio.dto.response.ClassReviewResponse;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.ClassReview;
import com.ga.gymio.model.FitnessClass;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.ClassReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassReviewServiceTest {

    @Mock
    private ClassReviewRepository classReviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ClassReviewService classReviewService;

    private Booking booking;
    private User customer;
    private FitnessClass fitnessClass;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setEmail("customer@example.com");

        fitnessClass = new FitnessClass();
        fitnessClass.setId(10L);

        booking = new Booking();
        booking.setId(20L);
        booking.setCustomer(customer);
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.COMPLETED);
    }

    @Test
    void createReview_whenBookingIsCompleted_savesReview() {
        ClassReviewRequest request = new ClassReviewRequest();
        request.setBookingId(20L);
        request.setRating(5);
        request.setComment("Excellent class!");

        when(bookingRepository.findById(20L))
                .thenReturn(Optional.of(booking));
        when(classReviewRepository.existsByBooking_Id(20L))
                .thenReturn(false);
        when(classReviewRepository.save(any(ClassReview.class)))
                .thenAnswer(invocation -> {
                    ClassReview review = invocation.getArgument(0);
                    review.setId(30L);
                    return review;
                });

        ClassReviewResponse response = classReviewService.createReview(
                request, "customer@example.com");

        assertEquals(30L, response.getId());
        assertEquals(20L, response.getBookingId());
        assertEquals(5, response.getRating());
        assertEquals("Excellent class!", response.getComment());

        verify(classReviewRepository).save(any(ClassReview.class));
    }

    @Test
    void createReview_whenBookingDoesNotExist_throwsNotFound() {
        ClassReviewRequest request = new ClassReviewRequest();
        request.setBookingId(20L);
        request.setRating(5);

        when(bookingRepository.findById(20L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.createReview(
                        request, "customer@example.com"));

        assertEquals(404, exception.getStatusCode().value());
        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenCustomerDoesNotOwnBooking_throwsForbidden() {
        ClassReviewRequest request = new ClassReviewRequest();
        request.setBookingId(20L);
        request.setRating(5);

        when(bookingRepository.findById(20L))
                .thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.createReview(
                        request, "other@example.com"));

        assertEquals(403, exception.getStatusCode().value());
        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenBookingIsNotCompleted_throwsBadRequest() {
        booking.setStatus(Booking.Status.CONFIRMED);

        ClassReviewRequest request = new ClassReviewRequest();
        request.setBookingId(20L);
        request.setRating(5);

        when(bookingRepository.findById(20L))
                .thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.createReview(
                        request, "customer@example.com"));

        assertEquals(400, exception.getStatusCode().value());
        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenReviewAlreadyExists_throwsConflict() {
        ClassReviewRequest request = new ClassReviewRequest();
        request.setBookingId(20L);
        request.setRating(4);

        when(bookingRepository.findById(20L))
                .thenReturn(Optional.of(booking));
        when(classReviewRepository.existsByBooking_Id(20L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.createReview(
                        request, "customer@example.com"));

        assertEquals(409, exception.getStatusCode().value());
        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenRatingIsInvalid_throwsBadRequest() {
        ClassReviewRequest request = new ClassReviewRequest();
        request.setBookingId(20L);
        request.setRating(6);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.createReview(
                        request, "customer@example.com"));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(bookingRepository);
        verify(classReviewRepository, never()).save(any());
    }

    @Test
    void deleteReview_whenCustomerOwnsReview_deletesReview() {
        ClassReview review = new ClassReview();
        review.setId(30L);
        review.setBooking(booking);

        when(classReviewRepository.findById(30L))
                .thenReturn(Optional.of(review));

        assertDoesNotThrow(() -> classReviewService.deleteReview(
                30L, "customer@example.com"));

        verify(classReviewRepository).delete(review);
    }

    @Test
    void deleteReview_whenCustomerDoesNotOwnReview_throwsForbidden() {
        ClassReview review = new ClassReview();
        review.setId(30L);
        review.setBooking(booking);

        when(classReviewRepository.findById(30L))
                .thenReturn(Optional.of(review));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> classReviewService.deleteReview(
                        30L, "other@example.com"));

        assertEquals(403, exception.getStatusCode().value());
        verify(classReviewRepository, never()).delete(any());
    }
}
