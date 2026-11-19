package com.ga.membership_payment_service.exception;

public class InformationExistsException extends RuntimeException {
    public InformationExistsException(String message)
    {
        super(message);
    }
}