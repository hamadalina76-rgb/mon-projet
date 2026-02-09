package com.speedline.user.exception;

import java.math.BigDecimal;

/**
 * Exception levée quand le solde du wallet est insuffisant
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public static InsufficientBalanceException forWallet(BigDecimal requested, BigDecimal available) {
        return new InsufficientBalanceException(
                String.format("Solde insuffisant. Demandé: %.2f, Disponible: %.2f", requested, available)
        );
    }
}
