package com.speedline.partner.exception;

/**
 * Levée quand une ressource demandée est introuvable.
 * Traduite en HTTP 404 par {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(resourceType + " introuvable avec l'identifiant : " + id);
    }
}
