package com.ga.notification_service.messaging;

import com.ga.notification_service.config.KafkaTopicConfig;
import com.ga.notification_service.event.PasswordResetEmailRequestedEvent;
import com.ga.notification_service.event.VerificationEmailRequestedEvent;
import com.ga.notification_service.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {

    private final EmailService emailService;

    @KafkaListener(
            topics = KafkaTopicConfig.VERIFICATION_EMAIL_TOPIC,
            groupId = "notification-service-group",
            containerFactory = "verificationEmailListenerFactory"
    )
    public void handleVerificationEmail(VerificationEmailRequestedEvent event) {
        log.info("Received verification email event");

        try {
            emailService.sendVerificationEmail(event.email(), event.token());
            log.info("Verification email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
            throw e;
        }
    }

    @KafkaListener(
            topics = KafkaTopicConfig.PASSWORD_RESET_EMAIL_TOPIC,
            groupId = "notification-service-group",
            containerFactory = "passwordResetEmailListenerFactory"
    )
    public void handlePasswordResetEmail(PasswordResetEmailRequestedEvent event) {
        log.info("Received password reset email event");

        try {
            emailService.sendPasswordResetEmail(event.email(), event.token());
            log.info("Password reset email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            throw e;
        }
    }
}