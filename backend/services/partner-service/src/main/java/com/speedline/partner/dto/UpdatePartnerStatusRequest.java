package com.speedline.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for PATCH /partners/{id}/status (toggle ouvert/fermé).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePartnerStatusRequest {
    /**
     * true = établissement ouvert (accepte les commandes), false = fermé.
     */
    private Boolean isOpen;
}
