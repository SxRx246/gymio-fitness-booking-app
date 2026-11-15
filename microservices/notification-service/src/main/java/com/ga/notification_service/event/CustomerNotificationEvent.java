package com.ga.notification_service.event;

/**
 * Represents a notification that the Fitness & Booking Service
 * requests the Notification Service to deliver to a customer.
 *
 * @param customerId the ID of the customer receiving the notification
 * @param message the notification message
 */
public record CustomerNotificationEvent(
        Long customerId,
        String message
) {
}