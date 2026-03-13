package com.speedline.user.domain;

/**
 * Statuts possibles pour un livreur
 */
public enum CourierStatus {
    
    /**
     * En attente de validation des documents
     */
    PENDING_APPROVAL,
    
    /**
     * Compte actif et validé
     */
    ACTIVE,
    
    /**
     * En ligne et disponible pour des livraisons
     */
    AVAILABLE,
    
    /**
     * En cours de livraison
     */
    BUSY,
    
    /**
     * Hors ligne (pause ou fin de service)
     */
    OFFLINE,
    
    /**
     * Compte suspendu temporairement
     */
    SUSPENDED,
    
    /**
     * Inscription rejetée par l'admin
     */
    REJECTED,

    /**
     * Compte désactivé définitivement
     */
    DEACTIVATED
}
