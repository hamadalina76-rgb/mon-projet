package com.speedline.partner.domain;

/**
 * Type de commission pour un partenaire
 */
public enum CommissionType {
    
    /**
     * Commission calculée en pourcentage du montant total de la commande
     */
    PERCENTAGE,
    
    /**
     * Montant ajouté au prix final (markup)
     */
    MARKUP
}