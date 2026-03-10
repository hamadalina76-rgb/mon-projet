package com.speedline.partner.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntryDTO {

    private Long          id;
    private Long          adminId;
    private String        adminName;    // "Admin #N" — enrichi si user-service disponible
    private String        adminRole;    // ex: "System Admin"
    private String        action;       // CREATE | UPDATE | DELETE | ACTIVATE | DEACTIVATE
    private LocalDateTime timestamp;
    private String        changesBefore;
    private String        changesAfter;
    private String        status;       // "SUCCESS"
}
