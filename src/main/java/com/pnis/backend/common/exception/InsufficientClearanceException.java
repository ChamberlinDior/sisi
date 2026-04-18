package com.pnis.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class InsufficientClearanceException extends RuntimeException {
    public InsufficientClearanceException(String message) { super(message); }
    public InsufficientClearanceException() {
        super("Niveau d'habilitation insuffisant pour accéder à cette ressource.");
    }
}
