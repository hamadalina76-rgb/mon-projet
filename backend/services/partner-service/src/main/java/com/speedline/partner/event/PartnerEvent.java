package com.speedline.partner.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Partner event for status changes (published via REST to notification-service)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerEvent {

    public enum EventType {
        PARTNER_REQUEST_SUBMITTED,
        PARTNER_APPROVED,
        PARTNER_REJECTED,
        PARTNER_SUSPENDED,
        PARTNER_ACTIVATED,
        PARTNER_DEACTIVATED,
        PARTNER_INFO_REQUESTED,

        // ==================== PRODUCT MODERATION ====================
        /**
         * A partner created/updated a product and it must be reviewed by an admin.
         */
        PRODUCT_REQUEST_SUBMITTED,

        /** Admin validated a product (partner should be notified). */
        PRODUCT_APPROVED,

        /** Admin rejected a product (partner should be notified with a reason). */
        PRODUCT_REJECTED
    }

    private EventType eventType;
    private Long partnerId;
    private Long userId;
    private String businessName;
    private String brandName;
    private String email; // Partner's email address
    private String status;
    private String reason;
    private LocalDateTime timestamp;

    // Product moderation payload
    private Long productId;
    private String productName;
    /**
     * New moderation status after admin action (or PENDING for submitted requests).
     * Serialized as string (PENDING/APPROVED/REJECTED).
     */
    private String newModerationStatus;
}
