package com.TravelBuddy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidInputsException extends RuntimeException {
    public InvalidInputsException(String message) {
        super(message);
    }
}
