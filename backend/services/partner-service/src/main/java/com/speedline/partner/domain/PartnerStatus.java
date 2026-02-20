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
     * Informations/documents complémentaires demandés par l'admin
     */
    DOCUMENTS_MISSING,
    
    /**
     * Compte actif et validé
     */
    ACTIVE,
    
    /**
     * Compte inactif (désactivé)
     */
    INACTIVE,
    
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
