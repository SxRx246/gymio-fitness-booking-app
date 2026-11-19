package com.ga.fitness_booking_service.controller;

import com.ga.fitness_booking_service.dto.request.WaitlistEntryRequest;
import com.ga.fitness_booking_service.dto.response.WaitlistEntryResponse;
import com.ga.fitness_booking_service.service.WaitlistEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller responsible for managing fitness class waitlists.
 * Provides endpoints for customers to join waitlists, view their entries,
 * leave waitlists, and respond to available booking offers.
 */
@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class WaitlistEntryController {

    private final WaitlistEntryService waitlistEntryService;

    /**
     * Adds the authenticated customer to a fitness class waitlist.
     *
     * @param request the request containing the fitness class ID
     * @return the created waitlist entry with HTTP status 201
     */
    @PostMapping
    public ResponseEntity<WaitlistEntryResponse> joinWaitlist(
            @Valid @RequestBody WaitlistEntryRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(waitlistEntryService.joinWaitlist(request));
    }

    /**
     * Retrieves all waitlist entries belonging to the authenticated customer.
     *
     * @return the customer's waitlist entries with HTTP status 200
     */
    @GetMapping("/me")
    public ResponseEntity<List<WaitlistEntryResponse>> getMyWaitlist() {

        return ResponseEntity.ok(
                waitlistEntryService.getMyWaitlist()
        );
    }

    /**
     * Removes a waitlist entry belonging to the authenticated customer.
     *
     * @param entryId the ID of the waitlist entry to remove
     * @return an empty response with HTTP status 204
     */
    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> leaveWaitlist(
            @PathVariable Long entryId) {

        waitlistEntryService.leaveWaitlist(entryId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Accepts a waitlist offer and confirms the customer's booking.
     *
     * @param entryId the ID of the waitlist entry with the offer
     * @return the updated waitlist entry with HTTP status 200
     */
    @PostMapping("/{entryId}/accept")
    public ResponseEntity<WaitlistEntryResponse> acceptOffer(
            @PathVariable Long entryId) {

        return ResponseEntity.ok(
                waitlistEntryService.acceptOffer(entryId)
        );
    }

    /**
     * Declines a waitlist offer so the next eligible customer
     * can receive the available place.
     *
     * @param entryId the ID of the waitlist entry with the offer
     * @return the updated waitlist entry with HTTP status 200
     */
    @PostMapping("/{entryId}/decline")
    public ResponseEntity<WaitlistEntryResponse> declineOffer(
            @PathVariable Long entryId) {

        return ResponseEntity.ok(
                waitlistEntryService.declineOffer(entryId)
        );
    }
}
