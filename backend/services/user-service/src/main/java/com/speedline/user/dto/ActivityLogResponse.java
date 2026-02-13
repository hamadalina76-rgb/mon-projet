package com.speedline.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse d'un log d'activité
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    
    private Long id;
    private Long adminId;
    private String adminName;
    private String action;
    private String resource;
    private String resourceId;
    private String details;
    private LocalDateTime timestamp;
}
