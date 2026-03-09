package com.speedline.partner.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Résultat du bulk update stock (import CSV).
 */
@Data
@Builder
public class BulkStockUpdateResult {

    private int processed;
    private int success;
    private List<BulkStockError> errors;

    @Data
    @Builder
    public static class BulkStockError {
        private int row;
        private Long productId;
        private String message;
    }
}
