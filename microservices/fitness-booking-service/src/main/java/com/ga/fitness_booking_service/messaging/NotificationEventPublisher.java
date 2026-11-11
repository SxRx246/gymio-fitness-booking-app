package com.ga.fitness_booking_service.messaging;

import com.ga.fitness_booking_service.event.BookingConfirmedEvent;
import com.ga.fitness_booking_service.event.WaitlistOfferCreatedEvent;
import com.ga.fitness_booking_service.event.WaitlistOfferExpiredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes fitness and booking notification events to Kafka after
 * their associated database transactions commit successfully.
 */
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private static final String WAITLIST_OFFER_CREATED_TOPIC =
            "waitlist-offer-created";

    private static final String WAITLIST_OFFER_EXPIRED_TOPIC =
            "waitlist-offer-expired";

    private static final String BOOKING_CONFIRMED_TOPIC =
            "booking-confirmed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a waitlist offer creation event after transaction commit.
     *
     * @param event the event containing the customer and class information
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishWaitlistOfferCreated(
            WaitlistOfferCreatedEvent event
    ) {
        kafkaTemplate.send(
                WAITLIST_OFFER_CREATED_TOPIC,
                event.customerId().toString(),
                event
        );
    }

    /**
     * Publishes a waitlist offer expiration event after transaction commit.
     *
     * @param event the event containing the customer and class information
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishWaitlistOfferExpired(
            WaitlistOfferExpiredEvent event
    ) {
        kafkaTemplate.send(
                WAITLIST_OFFER_EXPIRED_TOPIC,
                event.customerId().toString(),
                event
        );
    }

    /**
     * Publishes a booking confirmation event after transaction commit.
     *
     * @param event the event containing the booking and customer information
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishBookingConfirmed(
            BookingConfirmedEvent event
    ) {
        kafkaTemplate.send(
                BOOKING_CONFIRMED_TOPIC,
                event.customerId().toString(),
                event
        );
    }
}
