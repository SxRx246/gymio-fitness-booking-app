package com.ga.gymio.controller;

import com.ga.gymio.dto.request.ClassReviewRequest;
import com.ga.gymio.dto.response.ClassReviewResponse;
import com.ga.gymio.service.ClassReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/class-reviews")
public class ClassReviewController {

    private final ClassReviewService classReviewService;

    public ClassReviewController(ClassReviewService classReviewService) {
        this.classReviewService = classReviewService;
    }

    @PostMapping
    public ResponseEntity<ClassReviewResponse> createReview(
            @Valid @RequestBody ClassReviewRequest request,
            Authentication authentication) {

        ClassReviewResponse response = classReviewService.createReview(
                request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ClassReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ClassReviewRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                classReviewService.updateReview(
                        reviewId, request, authentication.getName()));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {

        classReviewService.deleteReview(
                reviewId, authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ClassReviewResponse> getReviewByBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                classReviewService.getReviewByBooking(
                        bookingId, authentication.getName()));
    }

    @GetMapping("/fitness-class/{fitnessClassId}")
    public ResponseEntity<List<ClassReviewResponse>> getReviewsByFitnessClass(
            @PathVariable Long fitnessClassId) {

        return ResponseEntity.ok(
                classReviewService.getReviewsByFitnessClass(fitnessClassId));
    }
}
