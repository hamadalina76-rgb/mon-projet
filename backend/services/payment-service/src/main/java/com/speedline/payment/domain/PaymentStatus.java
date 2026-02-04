package com.speedline.payment.domain;

/**
 * Statuts possibles pour un paiement
 */
public enum PaymentStatus {
    
    /**
     * Paiement en attente
     */
    PENDING,
    
    /**
     * Paiement en cours de traitement
     */
    PROCESSING,
    
    /**
     * Paiement complété avec succès
     */
    COMPLETED,
    
    /**
     * Paiement échoué
     */
    FAILED,
    
    /**
     * Paiement annulé
     */
    CANCELLED,
    
    /**
     * Paiement remboursé intégralement
     */
    REFUNDED,
    
    /**
     * Paiement partiellement remboursé
     */
    PARTIALLY_REFUNDED
}
