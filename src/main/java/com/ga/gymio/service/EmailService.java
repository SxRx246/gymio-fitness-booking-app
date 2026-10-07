package com.ga.gymio.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public void sendPasswordResetEmail(
            String email,
            String token
    ) {

        String resetLink =
                "http://localhost:8080/auth/users/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject("Reset your Gymio password");

        message.setText(
                "Hello!\n\n" +
                        "We received a request to reset your Gymio password.\n\n" +
                        "Click the link below to reset your password:\n\n" +
                        resetLink + "\n\n" +
                        "This link is valid for 15 minutes.\n\n" +
                        "If you did not request a password reset, please ignore this email.\n\n" +
                        "Thank you."
        );

        mailSender.send(message);
    }

    public void sendBookingConfirmationEmail( String email, String className, String startTime, String endTime ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' h:mm a");

        String formattedStartTime = LocalDateTime.parse(startTime).format(formatter);

        String formattedEndTime = LocalDateTime.parse(endTime).format(formatter);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject("Gymio booking confirmed");

        message.setText(
                "Hello!\n\n" + "Your booking has been confirmed.\n\n" +
                "Class: " + className + "\n" +
                "Start time: " + formattedStartTime + "\n" +
                "End time: " + formattedEndTime + "\n\n" +
                "We look forward to seeing you at Gymio!\n\n" + "Thank you." );

        mailSender.send(message);
    }

    public void sendBookingCancellationEmail(
            String email,
            String className,
            String startTime,
            String endTime
    ) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' h:mm a");

        String formattedStartTime =
                LocalDateTime.parse(startTime).format(formatter);

        String formattedEndTime =
                LocalDateTime.parse(endTime).format(formatter);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject("Gymio booking cancelled");

        message.setText(
                "Hello!\n\n" +
                        "Your booking has been cancelled.\n\n" +
                        "Class: " + className + "\n" +
                        "Start time: " + formattedStartTime + "\n" +
                        "End time: " + formattedEndTime + "\n\n" +
                        "If you did not request this cancellation, please contact Gymio.\n\n" +
                        "Thank you."
        );

        mailSender.send(message);
    }

    public void sendBookingStatusChangeEmail(
            String email,
            String className,
            String startTime,
            String endTime
    ) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' h:mm a");

        String formattedStartTime =
                LocalDateTime.parse(startTime).format(formatter);

        String formattedEndTime =
                LocalDateTime.parse(endTime).format(formatter);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject("Gymio booking completed");

        message.setText(
                "Hello!\n\n" +
                        "Your Gymio booking has been completed.\n\n" +
                        "Class: " + className + "\n" +
                        "Start time: " + formattedStartTime + "\n" +
                        "End time: " + formattedEndTime + "\n\n" +
                        "Thank you for training with Gymio!"
        );

        mailSender.send(message);
    }

    public void sendFitnessClassCancellationEmail(
            String email,
            String className,
            String startTime,
            String endTime
    ) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' h:mm a");

        String formattedStartTime =
                LocalDateTime.parse(startTime).format(formatter);

        String formattedEndTime =
                LocalDateTime.parse(endTime).format(formatter);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject("Gymio fitness class cancelled");

        message.setText(
                "Hello!\n\n" +
                        "We are sorry to inform you that your booked fitness class has been cancelled.\n\n" +
                        "Class: " + className + "\n" +
                        "Start time: " + formattedStartTime + "\n" +
                        "End time: " + formattedEndTime + "\n\n" +
                        "Your booking is no longer active.\n\n" +
                        "We apologize for the inconvenience.\n\n" +
                        "Thank you."
        );

        mailSender.send(message);
    }

}