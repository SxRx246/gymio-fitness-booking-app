package com.ga.gymio.controller;

import com.ga.gymio.dto.response.BookingResponse;
import com.ga.gymio.model.Booking;
import com.ga.gymio.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Provides REST endpoints for creating, retrieving, cancelling,
 * and deleting fitness class bookings.
 *
 * Access to each endpoint is controlled by the user's role.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    /**
     * Creates a booking for the specified fitness class.
     * Only customers can access this endpoint.
     *
     * @param fitnessClassId the ID of the fitness class to book
     * @return the created booking details
     */
    @PostMapping("/fitness-class/{fitnessClassId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public BookingResponse createBooking(@PathVariable Long fitnessClassId) {
        return bookingService.createBooking(fitnessClassId);
    }

    /**
     * Retrieves the authenticated customer's bookings in paginated form.
     *
     * @param pageable pagination and sorting information; defaults to
     *                 10 bookings per page
     * @return a page of the customer's booking details
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Page<BookingResponse> getMyBookings(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return bookingService.getMyBookings(pageable);
    }

    /**
     * Retrieves a booking by its ID.
     * Only customers can access this endpoint.
     *
     * @param id the ID of the booking to retrieve
     * @return the booking details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BookingResponse getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }

    /**
     * Cancels a booking belonging to the authenticated customer.
     *
     * @param id the ID of the booking to cancel
     */
    @PutMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CUSTOMER')")
    public void cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
    }

    /**
     * Retrieves bookings associated with a specific fitness class.
     * Trainers and administrators can access this endpoint.
     *
     * @param fitnessClassId the ID of the fitness class
     * @return a list of bookings for the specified class
     */
    @GetMapping("/fitness-class/{fitnessClassId}")
    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    public List<BookingResponse> getBookingsByFitnessClass(@PathVariable Long fitnessClassId) {
        return bookingService.getBookingsByFitnessClass(fitnessClassId);
    }

    /**
     * Retrieves all bookings in the system.
     * Only administrators can access this endpoint.
     *
     * @return a list of all booking details
     */
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> getAllBookings() {
        return bookingService.getAllBookings();
    }

    /**
     * Deletes a booking from the system.
     * Only administrators can access this endpoint.
     *
     * @param id the ID of the booking to delete
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
    }

}
