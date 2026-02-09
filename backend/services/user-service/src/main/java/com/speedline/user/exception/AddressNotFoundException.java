package com.speedline.user.exception;

/**
 * Exception levée quand une adresse n'est pas trouvée
 */
public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(String message) {
        super(message);
    }

    public static AddressNotFoundException byId(Long addressId) {
        return new AddressNotFoundException("Adresse non trouvée avec l'ID: " + addressId);
    }

    public static AddressNotFoundException noDefaultAddress(Long customerId) {
        return new AddressNotFoundException("Aucune adresse par défaut trouvée pour le client ID: " + customerId);
    }
}
