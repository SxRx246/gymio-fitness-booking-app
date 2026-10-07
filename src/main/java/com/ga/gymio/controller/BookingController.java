package com.ga.gymio.controller;

import com.ga.gymio.dto.response.BookingResponse;
import com.ga.gymio.model.Booking;
import com.ga.gymio.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/fitness-class/{fitnessClassId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public BookingResponse createBooking(@PathVariable Long fitnessClassId) {
        return bookingService.createBooking(fitnessClassId);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<BookingResponse> getMyBookings() {
        return bookingService.getMyBookings();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BookingResponse getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }

    @PutMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CUSTOMER')")
    public void cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
    }

    //    admin - trainer
    @GetMapping("/fitness-class/{fitnessClassId}")
    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    public List<BookingResponse> getBookingsByFitnessClass(@PathVariable Long fitnessClassId) {
        return bookingService.getBookingsByFitnessClass(fitnessClassId);
    }

    //    admin
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
    }

}
