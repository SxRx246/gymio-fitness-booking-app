package com.ga.notification_service.config;


import com.ga.notification_service.event.CustomerNotificationEvent;
import com.ga.notification_service.event.PasswordResetEmailRequestedEvent;
import com.ga.notification_service.event.VerificationEmailRequestedEvent;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ConsumerFactory<String, VerificationEmailRequestedEvent>
    verificationEmailConsumerFactory() {

        JacksonJsonDeserializer<VerificationEmailRequestedEvent> deserializer =
                new JacksonJsonDeserializer<>(
                        VerificationEmailRequestedEvent.class
                ).ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConsumerFactory<String, PasswordResetEmailRequestedEvent>
    passwordResetEmailConsumerFactory() {

        JacksonJsonDeserializer<PasswordResetEmailRequestedEvent> deserializer =
                new JacksonJsonDeserializer<>(
                        PasswordResetEmailRequestedEvent.class
                ).ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VerificationEmailRequestedEvent>
    verificationEmailListenerFactory() {

        var factory = new ConcurrentKafkaListenerContainerFactory<
                String, VerificationEmailRequestedEvent>();

        factory.setConsumerFactory(verificationEmailConsumerFactory());

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PasswordResetEmailRequestedEvent>
    passwordResetEmailListenerFactory() {

        var factory = new ConcurrentKafkaListenerContainerFactory<
                String, PasswordResetEmailRequestedEvent>();

        factory.setConsumerFactory(passwordResetEmailConsumerFactory());

        return factory;
    }

    /**
     * Creates a consumer factory for customer notification events.
     *
     * @return the customer notification consumer factory
     */
    @Bean
    public ConsumerFactory<String, CustomerNotificationEvent>
    customerNotificationConsumerFactory() {

        JacksonJsonDeserializer<CustomerNotificationEvent> deserializer =
                new JacksonJsonDeserializer<>(
                        CustomerNotificationEvent.class
                ).ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(),
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * Creates a Kafka listener container factory for customer notifications.
     *
     * @return the customer notification listener factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CustomerNotificationEvent>
    customerNotificationListenerFactory() {

        var factory = new ConcurrentKafkaListenerContainerFactory<
                String, CustomerNotificationEvent>();

        factory.setConsumerFactory(customerNotificationConsumerFactory());

        return factory;
    }
}
