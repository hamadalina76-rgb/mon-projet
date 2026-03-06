package com.speedline.partner.exception;

/**
 * Levée quand une règle métier est violée (ex : suppression d'une catégorie avec produits actifs).
 * Traduite en HTTP 422 par {@link GlobalExceptionHandler}.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
