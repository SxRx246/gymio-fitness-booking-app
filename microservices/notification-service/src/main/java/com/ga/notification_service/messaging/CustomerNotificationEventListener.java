package com.ga.notification_service.messaging;

import com.ga.notification_service.config.KafkaTopicConfig;
import com.ga.notification_service.event.CustomerNotificationEvent;
import com.ga.notification_service.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes customer notification events published by other microservices
 * and forwards them to the Notification Service for SSE delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerNotificationEventListener {

    private final NotificationService notificationService;

    /**
     * Handles customer notification events received from Kafka.
     *
     * @param event the customer notification event
     */
    @KafkaListener(
            topics = KafkaTopicConfig.CUSTOMER_NOTIFICATION_TOPIC,
            groupId = "notification-service-group",
            containerFactory = "customerNotificationListenerFactory"
    )
    public void handleCustomerNotification(CustomerNotificationEvent event) {

        log.info("Received customer notification event for customer ID: {}",
                event.customerId());

        notificationService.sendNotification(
                event.customerId(),
                event.message()
        );

        log.info("Customer notification processed successfully");
    }
}
