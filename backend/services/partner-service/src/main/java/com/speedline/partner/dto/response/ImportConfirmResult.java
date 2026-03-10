package com.speedline.partner.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Résultat de l'import CSV menu confirmé (TC-59). Erreurs par ligne, traitement continu.
 */
@Data
@Builder
public class ImportConfirmResult {

    private int processed;
    private int success;
    private List<ImportConfirmError> errors;

    @Data
    @Builder
    public static class ImportConfirmError {
        private int row;
        private Long productId;
        private String message;
    }
}
