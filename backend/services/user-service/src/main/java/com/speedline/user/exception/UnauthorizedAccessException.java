package com.speedline.user.exception;

/**
 * Exception levée lors d'un accès non autorisé à une ressource
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public static UnauthorizedAccessException addressNotOwnedByCustomer(Long addressId, Long customerId) {
        return new UnauthorizedAccessException(
                String.format("L'adresse ID %d n'appartient pas au client ID %d", addressId, customerId)
        );
    }

    public static UnauthorizedAccessException customerNotOwner(Long customerId) {
        return new UnauthorizedAccessException(
                String.format("Le client ID %d n'est pas autorisé à accéder à cette ressource", customerId)
        );
    }
}
