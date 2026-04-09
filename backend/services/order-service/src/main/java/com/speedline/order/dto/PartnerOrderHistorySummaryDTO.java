package com.speedline.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Agrégats pour l’écran historique partenaire (période {@code orderTime}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerOrderHistorySummaryDTO {

    private long totalOrders;
    private BigDecimal revenueTnd;
    private long cancelledCount;
    /** Pourcentage d’annulations sur le total de la période (0–100). */
    private double cancellationRatePercent;
}
