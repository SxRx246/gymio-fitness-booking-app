package com.ga.membership_payment_service.exception;

public class InformationNotFoundException extends RuntimeException{
    public InformationNotFoundException(String message){
        super(message);
    }
}