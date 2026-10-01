package com.ga.gymio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class InformationExistsException extends RuntimeException {
    public InformationExistsException(String message)
    {
        super(message);
    }
}