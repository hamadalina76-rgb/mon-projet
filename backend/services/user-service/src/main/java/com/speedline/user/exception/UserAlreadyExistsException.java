package com.speedline.user.exception;

/**
 * Exception levée quand un utilisateur existe déjà
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }

    public static UserAlreadyExistsException byUserId(Long userId) {
        return new UserAlreadyExistsException("Un profil client existe déjà pour l'utilisateur avec l'ID: " + userId);
    }
}
