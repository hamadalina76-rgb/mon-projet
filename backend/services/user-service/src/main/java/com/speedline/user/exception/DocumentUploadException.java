package com.speedline.user.exception;

/**
 * Exception levée lors d'une erreur d'upload de document
 */
public class DocumentUploadException extends RuntimeException {

    public DocumentUploadException(String message) {
        super(message);
    }

    public static DocumentUploadException invalidDocumentType(String documentType) {
        return new DocumentUploadException(
                String.format("Type de document invalide: %s. Types autorisés: CIN, LICENSE, INSURANCE, VEHICLE, PROFILE_PHOTO", documentType)
        );
    }

    public static DocumentUploadException missingDocumentUrl() {
        return new DocumentUploadException("L'URL du document est obligatoire.");
    }

    public static DocumentUploadException uploadFailed(String reason) {
        return new DocumentUploadException("Échec de l'upload du document: " + reason);
    }

    public static DocumentUploadException invalidFormat(String format) {
        return new DocumentUploadException(
                String.format("Format de fichier non supporté: %s. Formats autorisés: JPG, PNG, PDF", format)
        );
    }
}
