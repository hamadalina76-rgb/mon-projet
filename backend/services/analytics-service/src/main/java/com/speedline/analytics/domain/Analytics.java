package com.speedline.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité Analytics - Données analytiques pour ClickHouse
 * Note: ClickHouse n'utilise pas JPA standard, mais une structure similaire
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analytics {

    /**
     * Date de l'événement
     */
    private LocalDate date;

    /**
     * Heure de l'événement (0-23)
     */
    private Integer hour;

    /**
     * ID du partenaire
     */
    private Long partnerId;

    /**
     * ID du client
     */
    private Long customerId;

    /**
     * ID de la commande
     */
    private Long orderId;

    /**
     * Statut de la commande
     */
    private String status;

    /**
     * Montant total
     */
    private BigDecimal total;

    /**
     * Frais de livraison
     */
    private BigDecimal deliveryFee;

    /**
     * Temps de livraison en minutes
     */
    private Integer deliveryTimeMinutes;

    /**
     * Timestamp de création
     */
    private LocalDateTime createdAt;
}
