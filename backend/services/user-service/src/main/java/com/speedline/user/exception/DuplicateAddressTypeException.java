package com.speedline.user.exception;

import com.speedline.user.domain.AddressType;

/**
 * Exception levée quand un client essaie de créer une deuxième adresse
 * du même type exclusif (HOME, WORK, APARTMENT).
 */
public class DuplicateAddressTypeException extends RuntimeException {

    public DuplicateAddressTypeException(AddressType type) {
        super("Vous avez déjà une adresse de type « " + label(type) + " ». Un seul exemplaire est autorisé.");
    }

    private static String label(AddressType type) {
        return switch (type) {
            case HOME      -> "Maison";
            case WORK      -> "Bureau";
            case APARTMENT -> "Appartement";
            default        -> type.name();
        };
    }
}
