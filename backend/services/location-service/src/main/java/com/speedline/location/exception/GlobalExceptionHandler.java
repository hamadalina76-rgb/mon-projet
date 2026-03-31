package com.speedline.location.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions pour le location-service
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ZoneNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleZoneNotFound(ZoneNotFoundException ex) {
        log.warn("Zone non trouvée: {}", ex.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "ZONE_NOT_FOUND");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidBoundaryException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBoundary(InvalidBoundaryException ex) {
        log.warn("Polygone invalide: {}", ex.getMessage());
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        boolean isSelfIntersection = msg.toLowerCase().contains("auto-intersection")
                || msg.toLowerCase().contains("self-intersection")
                || msg.toLowerCase().contains("n'est pas valide");
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", isSelfIntersection ? "SELF_INTERSECTION" : "INVALID_BOUNDARY");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ZoneOverlapException.class)
    public ResponseEntity<Map<String, Object>> handleZoneOverlap(ZoneOverlapException ex) {
        log.warn("Chevauchement de zone: {}", ex.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.CONFLICT.value());
        error.put("error", "ZONE_OVERLAP");
        error.put("message", ex.getMessage());
        if (ex.getOverlappingZoneId() != null) {
            error.put("overlappingZoneId", ex.getOverlappingZoneId());
            error.put("overlappingZoneName", ex.getOverlappingZoneName());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // Erreurs par champ (map fieldName → message)
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        err -> err.getField(),
                        err -> err.getDefaultMessage(),
                        (existing, replacement) -> existing   // garder le 1er message si doublon
                ));
        String summary = fieldErrors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));
        log.warn("Erreur de validation: {}", summary);
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "VALIDATION_ERROR");
        error.put("message", summary);
        error.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argument invalide: {}", ex.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "BAD_REQUEST");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Erreur inattendue", ex);
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("error", "INTERNAL_SERVER_ERROR");
        error.put("message", "Une erreur inattendue s'est produite");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
