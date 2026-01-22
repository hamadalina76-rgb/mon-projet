package com.speedline.auth.exception;

/**
 * Exception pour token expiré
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
