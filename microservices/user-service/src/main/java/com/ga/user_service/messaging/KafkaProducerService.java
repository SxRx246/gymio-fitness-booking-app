package com.ga.user_service.messaging;

import com.ga.user_service.event.PasswordResetEmailRequestedEvent;
import com.ga.user_service.event.VerificationEmailRequestedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.ga.user_service.config.KafkaTopicConfig.PASSWORD_RESET_EMAIL_TOPIC;
import static com.ga.user_service.config.KafkaTopicConfig.VERIFICATION_EMAIL_TOPIC;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendVerificationEmail(
            VerificationEmailRequestedEvent event) {

        kafkaTemplate.send(
                VERIFICATION_EMAIL_TOPIC,
                event.email(),
                event
        );

        log.info("Verification email event published to Kafka");
    }

    public void sendPasswordResetEmail(
            PasswordResetEmailRequestedEvent event) {

        kafkaTemplate.send(
                PASSWORD_RESET_EMAIL_TOPIC,
                event.email(),
                event
        );

        log.info("Password reset email event published to Kafka");
    }
}
