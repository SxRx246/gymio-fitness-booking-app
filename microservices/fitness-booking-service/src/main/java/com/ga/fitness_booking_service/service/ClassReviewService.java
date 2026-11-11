package com.ga.fitness_booking_service.service;

import com.ga.fitness_booking_service.dto.AuthenticatedUser;
import com.ga.fitness_booking_service.dto.request.ClassReviewRequest;
import com.ga.fitness_booking_service.dto.response.ClassReviewResponse;
import com.ga.fitness_booking_service.exception.ForbiddenException;
import com.ga.fitness_booking_service.exception.InformationNotFoundException;
import com.ga.fitness_booking_service.model.Booking;
import com.ga.fitness_booking_service.model.ClassReview;
import com.ga.fitness_booking_service.repository.BookingRepository;
import com.ga.fitness_booking_service.repository.ClassReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Manages customer reviews for completed fitness class bookings.
 * Ensures that customers can only review their own completed bookings
 * and that each booking has at most one review.
 */
@Service
@RequiredArgsConstructor
public class ClassReviewService {

    private final ClassReviewRepository classReviewRepository;
    private final BookingRepository bookingRepository;

    /**
     * Creates a review for a completed booking belonging to the authenticated customer.
     *
     * @param request the review request containing the booking ID, rating, and comment
     * @return the created review response
     * @throws InformationNotFoundException if the booking does not exist
     * @throws ForbiddenException if the booking does not belong to the customer
     * @throws ResponseStatusException if the booking is not completed,
     *         the rating is invalid, or a review already exists
     */
    @Transactional
    public ClassReviewResponse createReview(ClassReviewRequest request) {

        AuthenticatedUser currentUser = getCurrentCustomer();

        validateRating(request.getRating());

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() ->
                        new InformationNotFoundException("Booking not found.")
                );

        validateBookingOwner(booking, currentUser);
        validateCompletedBooking(booking);

        if (classReviewRepository.existsByBooking_Id(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This booking already has a review."
            );
        }

        ClassReview review = new ClassReview();
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return toResponse(classReviewRepository.save(review));
    }

    /**
     * Updates an existing review belonging to the authenticated customer.
     *
     * @param reviewId the ID of the review to update
     * @param request the updated review details
     * @return the updated review response
     * @throws InformationNotFoundException if the review does not exist
     * @throws ForbiddenException if the review does not belong to the customer
     * @throws ResponseStatusException if the rating is invalid or the booking ID changes
     */
    @Transactional
    public ClassReviewResponse updateReview(
            Long reviewId,
            ClassReviewRequest request) {

        AuthenticatedUser currentUser = getCurrentCustomer();

        validateRating(request.getRating());

        ClassReview review = classReviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new InformationNotFoundException("Review not found.")
                );

        validateBookingOwner(review.getBooking(), currentUser);
        validateCompletedBooking(review.getBooking());

        if (!review.getBooking().getId().equals(request.getBookingId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The booking ID cannot be changed."
            );
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return toResponse(classReviewRepository.save(review));
    }

    /**
     * Deletes a review belonging to the authenticated customer.
     *
     * @param reviewId the ID of the review to delete
     * @throws InformationNotFoundException if the review does not exist
     * @throws ForbiddenException if the review does not belong to the customer
     */
    @Transactional
    public void deleteReview(Long reviewId) {

        AuthenticatedUser currentUser = getCurrentCustomer();

        ClassReview review = classReviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new InformationNotFoundException("Review not found.")
                );

        validateBookingOwner(review.getBooking(), currentUser);

        classReviewRepository.delete(review);
    }

    /**
     * Retrieves a review for a booking belonging to the authenticated customer.
     *
     * @param bookingId the ID of the booking
     * @return the review response
     * @throws InformationNotFoundException if the booking or its review does not exist
     * @throws ForbiddenException if the booking does not belong to the customer
     */
    @Transactional(readOnly = true)
    public ClassReviewResponse getReviewByBooking(Long bookingId) {

        AuthenticatedUser currentUser = getCurrentCustomer();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new InformationNotFoundException("Booking not found.")
                );

        validateBookingOwner(booking, currentUser);

        ClassReview review = classReviewRepository.findByBooking_Id(bookingId)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "No review exists for this booking."
                        )
                );

        return toResponse(review);
    }

    /**
     * Retrieves all reviews for a fitness class, ordered from newest to oldest.
     *
     * @param fitnessClassId the ID of the fitness class
     * @return the list of reviews for the fitness class
     */
    @Transactional(readOnly = true)
    public List<ClassReviewResponse> getReviewsByFitnessClass(Long fitnessClassId) {

        return classReviewRepository
                .findAllByBooking_FitnessClass_IdOrderByCreatedAtDesc(fitnessClassId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Verifies that a booking belongs to the authenticated customer.
     *
     * @param booking the booking to validate
     * @param currentUser the authenticated customer
     * @throws ForbiddenException if the booking belongs to another customer
     */
    private void validateBookingOwner(
            Booking booking,
            AuthenticatedUser currentUser) {

        if (booking.getCustomerId() == null
                || !booking.getCustomerId().equals(currentUser.userId())) {

            throw new ForbiddenException(
                    "You can only review your own bookings."
            );
        }
    }

    /**
     * Verifies that the booking has been completed.
     *
     * @param booking the booking to validate
     * @throws ResponseStatusException if the booking is not completed
     */
    private void validateCompletedBooking(Booking booking) {

        if (booking.getStatus() != Booking.Status.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only completed bookings can be reviewed."
            );
        }
    }

    /**
     * Validates that a rating is between 1 and 5.
     *
     * @param rating the rating to validate
     * @throws ResponseStatusException if the rating is outside the permitted range
     */
    private void validateRating(Integer rating) {

        if (rating == null || rating < 1 || rating > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be between 1 and 5."
            );
        }
    }

    /**
     * Retrieves the authenticated user from the security context.
     *
     * @return the authenticated user identity
     * @throws IllegalStateException if the user is not authenticated
     *         or the principal is invalid
     */
    private AuthenticatedUser getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof AuthenticatedUser currentUser)) {

            throw new IllegalStateException(
                    "An authenticated user is required."
            );
        }

        return currentUser;
    }

    /**
     * Retrieves the authenticated customer and verifies the customer role.
     *
     * @return the authenticated customer identity
     * @throws ForbiddenException if the authenticated user is not a customer
     */
    private AuthenticatedUser getCurrentCustomer() {

        AuthenticatedUser currentUser = getCurrentUser();

        if (!"CUSTOMER".equalsIgnoreCase(currentUser.role())) {
            throw new ForbiddenException(
                    "Only customers can manage class reviews."
            );
        }

        return currentUser;
    }

    /**
     * Converts a class review entity into its response DTO.
     *
     * @param review the review to convert
     * @return the corresponding review response
     */
    private ClassReviewResponse toResponse(ClassReview review) {

        return new ClassReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
