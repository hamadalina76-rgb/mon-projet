package com.speedline.user.domain;

/**
 * Statut d'un administrateur
 */
public enum AdminStatus {
    /**
     * Administrateur actif
     */
    ACTIVE,

    /**
     * Administrateur inactif (temporairement désactivé)
     */
    INACTIVE,

    /**
     * En attente de validation
     */
    PENDING,

    /**
     * Suspendu (sanction temporaire)
     */
    SUSPENDED
}
