package com.speedline.user.exception;

/**
 * Exception levée quand un livreur ne peut pas être mis en disponibilité
 */
public class CourierNotAvailableException extends RuntimeException {

    public CourierNotAvailableException(String message) {
        super(message);
    }

    public static CourierNotAvailableException notApproved(Long courierId) {
        return new CourierNotAvailableException(
                String.format("Le livreur ID %d ne peut pas être mis en ligne. Le compte doit d'abord être approuvé.", courierId)
        );
    }

    public static CourierNotAvailableException suspended(Long courierId) {
        return new CourierNotAvailableException(
                String.format("Le livreur ID %d est suspendu et ne peut pas être mis en disponibilité.", courierId)
        );
    }

    public static CourierNotAvailableException inactive(Long courierId) {
        return new CourierNotAvailableException(
                String.format("Le livreur ID %d est inactif. Veuillez contacter le support.", courierId)
        );
    }

    public static CourierNotAvailableException documentsNotVerified(Long courierId) {
        return new CourierNotAvailableException(
                String.format("Le livreur ID %d doit faire vérifier ses documents avant de pouvoir être disponible.", courierId)
        );
    }
}
