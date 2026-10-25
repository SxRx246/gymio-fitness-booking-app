package com.ga.gymio.service;

import com.ga.gymio.dto.request.ClassReviewRequest;
import com.ga.gymio.dto.response.ClassReviewResponse;
import com.ga.gymio.model.Booking;
import com.ga.gymio.model.ClassReview;
import com.ga.gymio.repository.BookingRepository;
import com.ga.gymio.repository.ClassReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ClassReviewService {

    private final ClassReviewRepository classReviewRepository;
    private final BookingRepository bookingRepository;

    public ClassReviewService(
            ClassReviewRepository classReviewRepository,
            BookingRepository bookingRepository) {
        this.classReviewRepository = classReviewRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public ClassReviewResponse createReview(
            ClassReviewRequest request,
            String authenticatedEmail) {

        validateRating(request.getRating());

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found."));

        validateBookingOwner(booking, authenticatedEmail);
        validateCompletedBooking(booking);

        if (classReviewRepository.existsByBooking_Id(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This booking already has a review.");
        }

        ClassReview review = new ClassReview();
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return toResponse(classReviewRepository.save(review));
    }

    @Transactional
    public ClassReviewResponse updateReview(
            Long reviewId,
            ClassReviewRequest request,
            String authenticatedEmail) {

        validateRating(request.getRating());

        ClassReview review = classReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Review not found."));

        validateBookingOwner(review.getBooking(), authenticatedEmail);
        validateCompletedBooking(review.getBooking());

        if (!review.getBooking().getId().equals(request.getBookingId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The booking ID cannot be changed.");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return toResponse(classReviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(
            Long reviewId,
            String authenticatedEmail) {

        ClassReview review = classReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Review not found."));

        validateBookingOwner(review.getBooking(), authenticatedEmail);

        classReviewRepository.delete(review);
    }

    @Transactional
    public ClassReviewResponse getReviewByBooking(
            Long bookingId,
            String authenticatedEmail) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found."));

        validateBookingOwner(booking, authenticatedEmail);

        ClassReview review = classReviewRepository
                .findByBooking_Id(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No review exists for this booking."));

        return toResponse(review);
    }

    @Transactional
    public List<ClassReviewResponse> getReviewsByFitnessClass(
            Long fitnessClassId) {

        return classReviewRepository
                .findAllByBooking_FitnessClass_IdOrderByCreatedAtDesc(
                        fitnessClassId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateBookingOwner(
            Booking booking,
            String authenticatedEmail) {

        if (booking.getCustomer() == null
                || booking.getCustomer().getEmail() == null
                || !booking.getCustomer().getEmail()
                .equalsIgnoreCase(authenticatedEmail)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only review your own bookings.");
        }
    }

    private void validateCompletedBooking(Booking booking) {
        if (booking.getStatus() != Booking.Status.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only completed bookings can be reviewed.");
        }
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be between 1 and 5.");
        }
    }

    private ClassReviewResponse toResponse(ClassReview review) {
        return new ClassReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
