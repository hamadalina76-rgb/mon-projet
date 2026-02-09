package com.speedline.user.exception;

/**
 * Exception levée quand un client n'est pas trouvé
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }

    public static CustomerNotFoundException byId(Long customerId) {
        return new CustomerNotFoundException("Client non trouvé avec l'ID: " + customerId);
    }

    public static CustomerNotFoundException byUserId(Long userId) {
        return new CustomerNotFoundException("Client non trouvé pour l'utilisateur avec l'ID: " + userId);
    }
}
