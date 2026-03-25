package com.speedline.user.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierChangeLogDTO {

    private Long          id;
    private Long          courierId;
    private Long          adminId;
    private String        adminName;
    private String        action;

    // Statut
    private String        statusBefore;
    private String        statusAfter;

    // Type de livreur
    private String        courierTypeBefore;
    private String        courierTypeAfter;

    // Zones
    private String        zoneIdsBefore;
    private String        zoneIdsAfter;

    // Général
    private String        description;
    private String        reason;
    private LocalDateTime changedAt;
}
