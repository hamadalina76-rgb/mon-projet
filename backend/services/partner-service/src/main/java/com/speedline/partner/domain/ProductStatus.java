package com.speedline.partner.domain;

/**
 * Statuts possibles pour un produit
 */
public enum ProductStatus {
    
    /**
     * Produit actif et disponible
     */
    ACTIVE,
    
    /**
     * Produit temporairement indisponible
     */
    OUT_OF_STOCK,
    
    /**
     * Produit désactivé (masqué)
     */
    INACTIVE,
    
    /**
     * Produit supprimé (soft delete)
     */
    DELETED
}
