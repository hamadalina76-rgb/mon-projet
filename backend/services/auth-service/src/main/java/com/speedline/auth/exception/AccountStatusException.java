package com.speedline.auth.exception;

import lombok.Getter;

/**
 * Exception thrown when account status prevents login
 * (INACTIVE, SUSPENDED, DELETED)
 */
@Getter
public class AccountStatusException extends RuntimeException {
    private final String code;

    public AccountStatusException(String message) {
        super(message);
        this.code = "ACCOUNT_BLOCKED";
    }

    public AccountStatusException(String code, String message) {
        super(message);
        this.code = code;
    }
}
