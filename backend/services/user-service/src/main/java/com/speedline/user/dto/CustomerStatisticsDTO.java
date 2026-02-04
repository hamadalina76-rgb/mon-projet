package com.speedline.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour exposer les statistiques d'un client.
 *
 * Utilisé principalement par l'interface d'admin et éventuellement
 * par l'application cliente pour afficher le profil avancé.
 *
 * Ce DTO est volontairement riche pour servir de base de travail
 * aux stagiaires (ils pourront compléter la logique dans le service).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatisticsDTO {

    /**
     * Identifiant interne du profil client.
     */
    private Long customerId;

    /**
     * Identifiant de l'utilisateur dans auth-service.
     */
    private Long userId;

    /**
     * Nombre total de commandes passées par ce client.
     */
    private Long totalOrders;

    /**
     * Montant total dépensé par ce client sur la plateforme.
     */
    private BigDecimal totalAmountSpent;

    /**
     * Panier moyen (totalAmountSpent / totalOrders).
     */
    private BigDecimal averageOrderValue;

    /**
     * Date/heure de la dernière commande.
     */
    private LocalDateTime lastOrderAt;

    /**
     * Solde actuel du wallet du client.
     */
    private BigDecimal walletBalance;

    /**
     * Nombre actuel de points de fidélité.
     */
    private Integer loyaltyPoints;

    /**
     * Nombre de partenaires favoris.
     */
    private Integer favoritePartnersCount;

    /**
     * Nombre de produits favoris.
     */
    private Integer favoriteProductsCount;

    /**
     * Nombre de filleuls (parrainage réussi).
     */
    private Integer successfulReferralsCount;
}

