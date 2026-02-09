package com.speedline.user.exception;

/**
 * Exception levée quand les points de fidélité sont insuffisants
 */
public class InsufficientPointsException extends RuntimeException {

    public InsufficientPointsException(String message) {
        super(message);
    }

    public static InsufficientPointsException forPoints(int requested, int available) {
        return new InsufficientPointsException(
                String.format("Points insuffisants. Demandé: %d, Disponible: %d", requested, available)
        );
    }
}
