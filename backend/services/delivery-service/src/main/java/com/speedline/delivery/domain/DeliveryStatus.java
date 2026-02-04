package com.speedline.delivery.domain;

/**
 * Statuts possibles pour une livraison
 */
public enum DeliveryStatus {
    
    /**
     * Livraison créée, en attente d'assignation d'un livreur
     */
    PENDING,
    
    /**
     * Livreur assigné, en attente d'acceptation
     */
    ASSIGNED,
    
    /**
     * Livreur a accepté, en route vers le partenaire
     */
    ACCEPTED,
    
    /**
     * Livreur arrivé chez le partenaire
     */
    ARRIVED_AT_PICKUP,
    
    /**
     * Commande récupérée, en route vers le client
     */
    PICKED_UP,
    
    /**
     * En cours de livraison
     */
    IN_TRANSIT,
    
    /**
     * Livreur arrivé à destination
     */
    ARRIVED_AT_DROPOFF,
    
    /**
     * Livraison complétée avec succès
     */
    DELIVERED,
    
    /**
     * Livraison annulée
     */
    CANCELLED,
    
    /**
     * Livraison échouée (client absent, adresse incorrecte, etc.)
     */
    FAILED
}
