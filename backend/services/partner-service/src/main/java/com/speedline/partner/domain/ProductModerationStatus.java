package com.speedline.partner.domain;

/**
 * Moderation statuses for products created/updated by partners.
 */
public enum ProductModerationStatus {
    /**
     * Product is waiting for admin review.
     */
    PENDING,

    /**
     * Product is approved and can be shown to customers.
     */
    APPROVED,

    /**
     * Product was rejected by admin (with a reason).
     */
    REJECTED
}

