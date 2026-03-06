package com.speedline.user.exception;

/**
 * Exception levée quand un client essaie d'ajouter une adresse
 * dont les coordonnées GPS ou l'adresse formatée existent déjà
 * dans ses adresses actives.
 */
public class DuplicateAddressLocationException extends RuntimeException {

    public DuplicateAddressLocationException() {
        super("Cette adresse existe déjà dans votre liste. Veuillez choisir un emplacement différent.");
    }
}
