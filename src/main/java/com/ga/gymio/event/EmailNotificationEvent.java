package com.ga.gymio.event;

public record EmailNotificationEvent(
        EmailType type,
        String email,
        String className,
        String startTime,
        String endTime
) {
    public enum EmailType {
        BOOKING_CONFIRMATION,
        BOOKING_CANCELLATION,
        BOOKING_STATUS_CHANGE,
        FITNESS_CLASS_CANCELLATION
    }
}