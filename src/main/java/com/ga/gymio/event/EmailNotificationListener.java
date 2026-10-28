package com.ga.gymio.event;

import com.ga.gymio.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private static final Logger logger =
            LoggerFactory.getLogger(EmailNotificationListener.class);

    private final EmailService emailService;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailNotification(EmailNotificationEvent event) {
        try {
            switch (event.type()) {
                case BOOKING_CONFIRMATION ->
                        emailService.sendBookingConfirmationEmail(
                                event.email(),
                                event.className(),
                                event.startTime(),
                                event.endTime()
                        );

                case BOOKING_CANCELLATION ->
                        emailService.sendBookingCancellationEmail(
                                event.email(),
                                event.className(),
                                event.startTime(),
                                event.endTime()
                        );

                case BOOKING_STATUS_CHANGE ->
                        emailService.sendBookingStatusChangeEmail(
                                event.email(),
                                event.className(),
                                event.startTime(),
                                event.endTime()
                        );

                case FITNESS_CLASS_CANCELLATION ->
                        emailService.sendFitnessClassCancellationEmail(
                                event.email(),
                                event.className(),
                                event.startTime(),
                                event.endTime()
                        );
            }

            logger.info(
                    "Email notification processed: type={}, recipient={}",
                    event.type(),
                    event.email()
            );
        } catch (Exception exception) {
            logger.error(
                    "Failed to send email notification: type={}, recipient={}",
                    event.type(),
                    event.email(),
                    exception
            );
        }
    }
}