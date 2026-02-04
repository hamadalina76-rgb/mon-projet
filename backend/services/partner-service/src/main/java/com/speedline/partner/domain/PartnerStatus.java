package com.speedline.partner.domain;

/**
 * Statuts possibles pour un partenaire
 */
public enum PartnerStatus {
    
    /**
     * En attente de validation par l'admin
     */
    PENDING,
    
    /**
     * Compte actif et validé
     */
    ACTIVE,
    
    /**
     * Compte suspendu temporairement
     */
    SUSPENDED,
    
    /**
     * Compte rejeté lors de la validation
     */
    REJECTED,
    
    /**
     * Compte fermé définitivement
     */
    CLOSED
}
