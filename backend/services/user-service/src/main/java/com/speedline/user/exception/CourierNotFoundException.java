package com.speedline.user.exception;

/**
 * Exception levée quand un livreur n'est pas trouvé
 */
public class CourierNotFoundException extends RuntimeException {

    public CourierNotFoundException(String message) {
        super(message);
    }

    public static CourierNotFoundException byId(Long courierId) {
        return new CourierNotFoundException("Livreur non trouvé avec l'ID: " + courierId);
    }

    public static CourierNotFoundException byUserId(Long userId) {
        return new CourierNotFoundException("Livreur non trouvé pour l'utilisateur ID: " + userId);
    }
}
