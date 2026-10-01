package com.ga.gymio.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String email, String token) {

        String verificationLink =
                "http://localhost:8080/auth/users/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject("Verify your Gymio account");

        message.setText(
                "Welcome to Gymio!\n\n" +
                        "Please verify your email address by clicking the link below:\n\n" +
                        verificationLink + "\n\n" +
                        "This verification link is valid for 24 hours.\n\n" +
                        "If you did not create a Gymio account, please ignore this email."
        );

        mailSender.send(message);
    }
}