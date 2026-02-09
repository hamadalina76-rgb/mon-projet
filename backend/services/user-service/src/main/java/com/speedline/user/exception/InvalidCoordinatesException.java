package com.speedline.user.exception;

import java.math.BigDecimal;

/**
 * Exception levée quand les coordonnées GPS sont invalides
 */
public class InvalidCoordinatesException extends RuntimeException {

    public InvalidCoordinatesException(String message) {
        super(message);
    }

    public static InvalidCoordinatesException invalidLatitude(BigDecimal latitude) {
        return new InvalidCoordinatesException(
                String.format("Latitude invalide: %s. La latitude doit être comprise entre -90 et 90.", latitude)
        );
    }

    public static InvalidCoordinatesException invalidLongitude(BigDecimal longitude) {
        return new InvalidCoordinatesException(
                String.format("Longitude invalide: %s. La longitude doit être comprise entre -180 et 180.", longitude)
        );
    }

    public static InvalidCoordinatesException missingCoordinates() {
        return new InvalidCoordinatesException(
                "Les coordonnées GPS sont incomplètes. La latitude et la longitude doivent être fournies ensemble."
        );
    }

    public static InvalidCoordinatesException addressNotGeocoded(Long addressId) {
        return new InvalidCoordinatesException(
                String.format("L'adresse ID %d n'a pas de coordonnées GPS. Veuillez d'abord géocoder l'adresse.", addressId)
        );
    }
}
