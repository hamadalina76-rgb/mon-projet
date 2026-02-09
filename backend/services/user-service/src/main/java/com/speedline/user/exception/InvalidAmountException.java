package com.speedline.user.exception;

import java.math.BigDecimal;

/**
 * Exception levée quand un montant est invalide (négatif ou nul)
 */
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }

    public static InvalidAmountException negativeOrZero(BigDecimal amount) {
        return new InvalidAmountException("Le montant doit être positif. Reçu: " + amount);
    }

    public static InvalidAmountException negativePoints(int points) {
        return new InvalidAmountException("Le nombre de points doit être positif. Reçu: " + points);
    }
}
