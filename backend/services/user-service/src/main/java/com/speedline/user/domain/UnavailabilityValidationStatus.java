package com.speedline.user.domain;

/**
 * Workflow de validation des declarations d'indisponibilite.
 */
public enum UnavailabilityValidationStatus {
    PENDING_VALIDATION,
    APPROVED_ACTIVE,
    REJECTED,
    RESOLVED_AVAILABLE
}
