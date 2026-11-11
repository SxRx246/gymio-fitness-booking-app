package com.ga.fitness_booking_service.event;

/**
 * Represents a confirmed booking created by accepting a waitlist offer.
 *
 * @param customerId the ID of the customer
 * @param email the customer's email address
 * @param bookingId the ID of the confirmed booking
 * @param className the name of the fitness class
 * @param startTime the class start time in ISO-8601 format
 * @param endTime the class end time in ISO-8601 format
 */
public record BookingConfirmedEvent(
        Long customerId,
        String email,
        Long bookingId,
        String className,
        String startTime,
        String endTime
) {
}
