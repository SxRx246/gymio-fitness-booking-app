package com.ga.fitness_booking_service.exception;

public class InformationExistsException extends RuntimeException {
    public InformationExistsException(String message)
    {
        super(message);
    }
}