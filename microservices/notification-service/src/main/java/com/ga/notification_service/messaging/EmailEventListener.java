package com.ga.notification_service.messaging;

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
            topics = "verification-email-requested",
            groupId = "notification-service-group"
    )
    public void handleVerificationEmail(
            VerificationEmailRequestedEvent event) {

        log.info("Received verification email event");

        emailService.sendVerificationEmail(
                event.email(),
                event.token()
        );
    }

    @KafkaListener(
            topics = "password-reset-email-requested",
            groupId = "notification-service-group"
    )
    public void handlePasswordResetEmail(
            PasswordResetEmailRequestedEvent event) {

        log.info("Received password reset email event");

        emailService.sendPasswordResetEmail(
                event.email(),
                event.token()
        );
    }
}