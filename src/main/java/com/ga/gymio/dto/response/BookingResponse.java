package com.ga.gymio.dto.response;

import com.ga.gymio.model.Booking;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BookingResponse {

    private Long id;
    private Booking.Status status;
    private LocalDateTime bookedAt;

    private Long customerId;
    private String customerEmail;

    private Long fitnessClassId;
    private String fitnessClassName;

    public BookingResponse(Booking booking) {
        this.id = booking.getId();
        this.status = booking.getStatus();
        this.bookedAt = booking.getBookedAt();

        this.customerId = booking.getCustomer().getId();
        this.customerEmail = booking.getCustomer().getEmail();

        this.fitnessClassId = booking.getFitnessClass().getId();
        this.fitnessClassName = booking.getFitnessClass().getName();
    }
}

