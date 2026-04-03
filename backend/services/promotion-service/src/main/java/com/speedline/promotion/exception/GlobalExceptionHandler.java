package com.speedline.promotion.exception;

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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PromotionException.class)
    public ResponseEntity<ErrorResponse> handlePromotionException(PromotionException ex) {
        log.warn("Promotion error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = resolveStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage(), status.value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field = err instanceof FieldError fe ? fe.getField() : err.getObjectName();
            errors.put(field, err.getDefaultMessage());
        });
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", errors.toString(), 400));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "Une erreur interne s'est produite", 500));
    }

    private HttpStatus resolveStatus(String errorCode) {
        return switch (errorCode) {
            case "PROMOTION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE_CODE" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    public record ErrorResponse(String code, String message, int status) {
        public String timestamp() { return LocalDateTime.now().toString(); }
    }
}
