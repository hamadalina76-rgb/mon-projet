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
        PARTNER_INFO_REQUESTED
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
}
