package com.speedline.partner.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerChangeLogDTO {

    private Long          id;
    private Long          partnerId;
    private Long          adminId;
    private String        adminName;
    private String        action;

    // Status
    private String        statusBefore;
    private String        statusAfter;

    // Commission
    private String        commissionTypeBefore;
    private String        commissionTypeAfter;
    private BigDecimal    commissionRateBefore;
    private BigDecimal    commissionRateAfter;

    // Categories
    private String        categoryIdsBefore;
    private String        categoryIdsAfter;

    // Product edit permission
    private Boolean productEditPermissionBefore;
    private Boolean productEditPermissionAfter;

    // General
    private String        reason;
    private LocalDateTime changedAt;
}
