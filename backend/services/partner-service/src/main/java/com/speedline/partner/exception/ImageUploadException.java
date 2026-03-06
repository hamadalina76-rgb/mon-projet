package com.speedline.partner.exception;

/**
 * Levée quand un upload d'image échoue (validation, stockage, etc.).
 * Les contrôleurs la traduisent en HTTP 400 ou 503 selon le cas.
 */
public class ImageUploadException extends RuntimeException {

    public ImageUploadException(String message) {
        super(message);
    }

    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
