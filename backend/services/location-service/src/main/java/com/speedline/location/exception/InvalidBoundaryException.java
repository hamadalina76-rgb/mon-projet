package com.speedline.location.exception;

/**
 * Exception levée lorsqu'un polygone de zone est invalide
 */
public class InvalidBoundaryException extends RuntimeException {
    
    public InvalidBoundaryException(String message) {
        super(message);
    }
    
    public InvalidBoundaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
