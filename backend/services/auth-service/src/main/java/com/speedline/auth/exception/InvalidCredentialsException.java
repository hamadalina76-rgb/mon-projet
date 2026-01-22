package com.speedline.auth.exception;

/**
 * Exception pour credentials invalides
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
