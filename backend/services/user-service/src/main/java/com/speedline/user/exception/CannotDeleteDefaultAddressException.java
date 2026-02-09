package com.speedline.user.exception;

/**
 * Exception levée quand on tente de supprimer une adresse par défaut
 * alors qu'il existe d'autres adresses
 */
public class CannotDeleteDefaultAddressException extends RuntimeException {

    public CannotDeleteDefaultAddressException(String message) {
        super(message);
    }

    public static CannotDeleteDefaultAddressException otherAddressesExist(Long addressId) {
        return new CannotDeleteDefaultAddressException(
                String.format(
                        "Impossible de supprimer l'adresse par défaut (ID: %d). " +
                        "Veuillez d'abord définir une autre adresse comme adresse par défaut.",
                        addressId
                )
        );
    }
}
