package com.speedline.partner.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Réponse du preview d'import CSV menu (TC-58).
 */
@Data
@Builder
public class ImportPreviewResponse {

    /** Modifications détectées (produit, champ, ancienne valeur, nouvelle valeur). */
    private List<ImportChangeRow> changes;

    /** Erreurs de parsing par ligne (ligne invalide, id manquant, etc.). */
    private List<ImportParseError> parseErrors;

    @Data
    @Builder
    public static class ImportChangeRow {
        private Long productId;
        private String productName;
        private String field;
        private String oldValue;
        private String newValue;
    }

    @Data
    @Builder
    public static class ImportParseError {
        private int row;
        private String message;
    }
}
