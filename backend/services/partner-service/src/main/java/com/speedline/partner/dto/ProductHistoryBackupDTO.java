package com.speedline.partner.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductHistoryBackupDTO {
    private Long id;
    private Long partnerId;
    private Long productId;
    private String action;
    private String actorType;
    private Long actorId;
    private String adminName;
    private String changesBefore;
    private String changesAfter;
    private String reason;
    private LocalDateTime createdAt;
}
