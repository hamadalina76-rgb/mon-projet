package com.speedline.user.domain;

/**
 * Type de livreur — défini par l'admin lors de l'approbation.
 */
public enum CourierType {

    /**
     * Livreur interne : salarié ou employé de SpeedLine.
     */
    INTERNAL,

    /**
     * Livreur externe : partenaire ou auto-entrepreneur indépendant.
     */
    EXTERNAL
}
