package com.ga.fitness_booking_service.event;

/**
 * Represents a notification that a waitlist offer has been created.
 *
 * @param customerId the ID of the customer receiving the offer
 * @param className the name of the fitness class
 * @param offerMinutes the number of minutes available to accept the offer
 */
public record WaitlistOfferCreatedEvent(
        Long customerId,
        String className,
        int offerMinutes
) {
}
