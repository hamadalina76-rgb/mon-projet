package com.speedline.user.exception;

/**
 * Exception levée quand un client essaie de créer une adresse
 * avec une étiquette (label) déjà utilisée dans ses adresses actives.
 * Toutes les adresses d'un même client doivent avoir des étiquettes uniques.
 */
public class DuplicateAddressLabelException extends RuntimeException {

    public DuplicateAddressLabelException(String label) {
        super("Vous avez déjà une adresse avec l'étiquette « " + label + " ». Choisissez un nom différent.");
    }
}
