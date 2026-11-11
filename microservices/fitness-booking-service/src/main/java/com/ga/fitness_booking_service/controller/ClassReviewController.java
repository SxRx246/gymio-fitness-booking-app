package com.ga.fitness_booking_service.controller;

import com.ga.fitness_booking_service.dto.request.ClassReviewRequest;
import com.ga.fitness_booking_service.dto.response.ClassReviewResponse;
import com.ga.fitness_booking_service.service.ClassReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Provides REST endpoints for creating, updating, deleting,
 * and retrieving fitness class reviews.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/class-reviews")
public class ClassReviewController {

    private final ClassReviewService classReviewService;

    /**
     * Creates a review for a completed booking.
     *
     * @param request the review details
     * @return the created review
     */
    @PostMapping
    public ResponseEntity<ClassReviewResponse> createReview(
            @Valid @RequestBody ClassReviewRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classReviewService.createReview(request));
    }

    /**
     * Updates an existing review.
     *
     * @param reviewId the ID of the review
     * @param request the updated review details
     * @return the updated review
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ClassReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ClassReviewRequest request) {

        return ResponseEntity.ok(
                classReviewService.updateReview(reviewId, request));
    }

    /**
     * Deletes an existing review.
     *
     * @param reviewId the ID of the review
     * @return an empty response with HTTP 204
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {

        classReviewService.deleteReview(reviewId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a review associated with a booking.
     *
     * @param bookingId the ID of the booking
     * @return the review details
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ClassReviewResponse> getReviewByBooking(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(
                classReviewService.getReviewByBooking(bookingId));
    }

    /**
     * Retrieves all reviews associated with a fitness class.
     *
     * @param fitnessClassId the ID of the fitness class
     * @return the list of reviews
     */
    @GetMapping("/fitness-class/{fitnessClassId}")
    public ResponseEntity<List<ClassReviewResponse>> getReviewsByFitnessClass(
            @PathVariable Long fitnessClassId) {

        return ResponseEntity.ok(
                classReviewService.getReviewsByFitnessClass(fitnessClassId));
    }
}
