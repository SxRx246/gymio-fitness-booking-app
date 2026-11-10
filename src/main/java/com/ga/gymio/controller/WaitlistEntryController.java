package com.ga.gymio.controller;

import com.ga.gymio.dto.request.WaitlistEntryRequest;
import com.ga.gymio.dto.response.WaitlistEntryResponse;
import com.ga.gymio.service.WaitlistEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class WaitlistEntryController {

    private final WaitlistEntryService waitlistEntryService;

    /**
     * Adds the customer to a fitness class waitlist.
     *
     * @param request the request containing the fitness class ID
     * @return a response containing the created waitlist entry
     *         with HTTP status 201 (Created)
     */
    @PostMapping
    public ResponseEntity<WaitlistEntryResponse> joinWaitlist(
            @Valid @RequestBody WaitlistEntryRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(waitlistEntryService.joinWaitlist(request));
    }

    /**
     * Retrieves the authenticated customer's waitlist entries.
     *
     * @return the customer's waitlist entries with HTTP status 200 (OK)
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
     * @return an empty response with HTTP status 204 (No Content)
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
     * @return the updated waitlist entry with HTTP status 200 (OK)
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
     * can be offered the available place.
     *
     * @param entryId the ID of the waitlist entry with the offer
     * @return the updated waitlist entry with HTTP status 200 (OK)
     */
    @PostMapping("/{entryId}/decline")
    public ResponseEntity<WaitlistEntryResponse> declineOffer(
            @PathVariable Long entryId) {

        return ResponseEntity.ok(
                waitlistEntryService.declineOffer(entryId)
        );
    }
}