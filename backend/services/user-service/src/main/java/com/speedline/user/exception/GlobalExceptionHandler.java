package com.speedline.user.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour le user-service
 * Centralise la gestion des erreurs et retourne des réponses standardisées
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Structure de réponse d'erreur standardisée
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> details
    ) {
        public ErrorResponse(int status, String error, String message) {
            this(LocalDateTime.now(), status, error, message, null, null);
        }

        public ErrorResponse(int status, String error, String message, Map<String, String> details) {
            this(LocalDateTime.now(), status, error, message, null, details);
        }
    }

    // ==================== EXCEPTIONS MÉTIER ====================

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex) {
        log.warn("Client non trouvé: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAddressNotFound(AddressNotFoundException ex) {
        log.warn("Adresse non trouvée: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        "ADDRESS_NOT_FOUND",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(DuplicateAddressTypeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAddressType(DuplicateAddressTypeException ex) {
        log.warn("Type d'adresse en doublon: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        "DUPLICATE_ADDRESS_TYPE",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(DuplicateAddressLabelException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAddressLabel(DuplicateAddressLabelException ex) {
        log.warn("Étiquette d'adresse en doublon: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        "DUPLICATE_ADDRESS_LABEL",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(DuplicateAddressLocationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAddressLocation(DuplicateAddressLocationException ex) {
        log.warn("Adresse physique en doublon: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        "DUPLICATE_ADDRESS_LOCATION",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidCoordinatesException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCoordinates(InvalidCoordinatesException ex) {
        log.warn("Coordonnées invalides: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "INVALID_COORDINATES",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        log.warn("Accès non autorisé: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        HttpStatus.FORBIDDEN.value(),
                        "FORBIDDEN",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(CannotDeleteDefaultAddressException.class)
    public ResponseEntity<ErrorResponse> handleCannotDeleteDefaultAddress(CannotDeleteDefaultAddressException ex) {
        log.warn("Suppression adresse par défaut: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "CANNOT_DELETE_DEFAULT_ADDRESS",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("Utilisateur existant: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        "CONFLICT",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAmount(InvalidAmountException ex) {
        log.warn("Montant invalide: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "BAD_REQUEST",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Solde insuffisant: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "INSUFFICIENT_BALANCE",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InsufficientPointsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPoints(InsufficientPointsException ex) {
        log.warn("Points insuffisants: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "INSUFFICIENT_POINTS",
                        ex.getMessage()
                ));
    }

    // ==================== EXCEPTIONS LIVREUR ====================

    @ExceptionHandler(CourierNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourierNotFound(CourierNotFoundException ex) {
        log.warn("Livreur non trouvé: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        "COURIER_NOT_FOUND",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(CourierNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleCourierNotAvailable(CourierNotAvailableException ex) {
        log.warn("Livreur non disponible: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "COURIER_NOT_AVAILABLE",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(DocumentUploadException.class)
    public ResponseEntity<ErrorResponse> handleDocumentUpload(DocumentUploadException ex) {
        log.warn("Erreur upload document: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "DOCUMENT_UPLOAD_ERROR",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidReferralCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReferralCode(InvalidReferralCodeException ex) {
        log.warn("Code de parrainage invalide: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "INVALID_REFERRAL_CODE",
                        ex.getMessage()
                ));
    }

    // ==================== EXCEPTIONS DE VALIDATION ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Erreur de validation: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        "Erreur de validation des données",
                        errors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Violation de contrainte: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "CONSTRAINT_VIOLATION",
                        "Violation de contrainte de validation",
                        errors
                ));
    }

    // ==================== EXCEPTIONS GÉNÉRIQUES ====================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argument illégal: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "BAD_REQUEST",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("❌ Erreur inattendue: ", ex);
        log.error("   Type: {}", ex.getClass().getName());
        log.error("   Message: {}", ex.getMessage());
        if (ex.getCause() != null) {
            log.error("   Cause: {}", ex.getCause().getMessage());
        }
        
        // Retourner plus de détails pour le debug (à sécuriser en production)
        String detailedMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_SERVER_ERROR",
                        "Erreur serveur: " + detailedMessage
                ));
    }
}
