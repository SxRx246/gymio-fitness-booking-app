package com.ga.fitness_booking_service.event;

/**
 * Represents a notification that a customer's waitlist offer has expired.
 *
 * @param customerId the ID of the customer whose offer expired
 * @param className the name of the fitness class
 */
public record WaitlistOfferExpiredEvent(
        Long customerId,
        String className
) {
}
