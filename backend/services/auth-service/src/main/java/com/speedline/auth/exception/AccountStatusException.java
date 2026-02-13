package com.speedline.auth.exception;

/**
 * Exception thrown when account status prevents login
 * (INACTIVE, SUSPENDED, DELETED)
 */
public class AccountStatusException extends RuntimeException {
    public AccountStatusException(String message) {
        super(message);
    }
}
