package com.speedline.user.exception;

/**
 * Exception levée quand un code de parrainage est invalide
 */
public class InvalidReferralCodeException extends RuntimeException {

    public InvalidReferralCodeException(String message) {
        super(message);
    }

    public static InvalidReferralCodeException notFound(String referralCode) {
        return new InvalidReferralCodeException("Code de parrainage invalide: " + referralCode);
    }

    public static InvalidReferralCodeException selfReferral() {
        return new InvalidReferralCodeException("Vous ne pouvez pas utiliser votre propre code de parrainage");
    }

    public static InvalidReferralCodeException alreadyUsed() {
        return new InvalidReferralCodeException("Vous avez déjà utilisé un code de parrainage");
    }
}
