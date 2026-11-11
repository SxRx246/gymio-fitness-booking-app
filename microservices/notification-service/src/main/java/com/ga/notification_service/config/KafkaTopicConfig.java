package com.ga.notification_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Defines Kafka topics used by the Notification Service.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String VERIFICATION_EMAIL_TOPIC =
            "verification-email-requested";

    public static final String PASSWORD_RESET_EMAIL_TOPIC =
            "password-reset-email-requested";

    public static final String CUSTOMER_NOTIFICATION_TOPIC =
            "customer-notification";

    /**
     * Creates the verification email topic.
     *
     * @return the verification email Kafka topic
     */
    @Bean
    public NewTopic verificationEmailTopic() {
        return TopicBuilder.name(VERIFICATION_EMAIL_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Creates the password reset email topic.
     *
     * @return the password reset email Kafka topic
     */
    @Bean
    public NewTopic passwordResetEmailTopic() {
        return TopicBuilder.name(PASSWORD_RESET_EMAIL_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Creates the customer notification topic.
     *
     * @return the customer notification Kafka topic
     */
    @Bean
    public NewTopic customerNotificationTopic() {
        return TopicBuilder.name(CUSTOMER_NOTIFICATION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
