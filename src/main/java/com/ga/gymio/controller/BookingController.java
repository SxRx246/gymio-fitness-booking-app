package com.ga.gymio.controller;

import com.ga.gymio.model.Booking;
import com.ga.gymio.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/fitness-classes/{fitnessClassId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public Booking createBooking(@PathVariable Long fitnessClassId) {
        return bookingService.createBooking(fitnessClassId);
    }


}
