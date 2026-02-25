package com.speedline.user.service;

import com.speedline.user.domain.ActivityLog;
import com.speedline.user.dto.ActivityLogRequest;
import com.speedline.user.dto.ActivityLogResponse;
import com.speedline.user.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service pour la gestion des logs d'activité
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {
    
    private final ActivityLogRepository activityLogRepository;
    
    /**
     * Crée un nouveau log d'activité
     */
    @Transactional
    public ActivityLogResponse createLog(ActivityLogRequest request) {
        log.info("Creating activity log: {} - {} on {}", 
                 request.getAdminName(), request.getAction(), request.getResource());
        
        ActivityLog log = ActivityLog.builder()
                .adminId(request.getAdminId())
                .adminName(request.getAdminName())
                .action(request.getAction())
                .resource(request.getResource())
                .resourceId(request.getResourceId())
                .details(request.getDetails())
                .timestamp(LocalDateTime.now())
                .build();
        
        ActivityLog saved = activityLogRepository.save(log);
        return mapToResponse(saved);
    }
    
    /**
     * Récupère tous les logs avec pagination
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getAllLogs(Pageable pageable) {
        log.info("Fetching all activity logs - page: {}", pageable.getPageNumber());
        return activityLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::mapToResponse);
    }
    
    /**
     * Récupère les logs d'un admin spécifique
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getLogsByAdmin(Long adminId, Pageable pageable) {
        log.info("Fetching logs for admin: {}", adminId);
        return activityLogRepository.findByAdminIdOrderByTimestampDesc(adminId, pageable)
                .map(this::mapToResponse);
    }
    
    /**
     * Récupère les logs par type de ressource
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getLogsByResource(String resource, Pageable pageable) {
        log.info("Fetching logs for resource: {}", resource);
        return activityLogRepository.findByResourceOrderByTimestampDesc(resource, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupère les logs pour une ressource et un ID donné (ex: client 123)
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getLogsByResourceAndResourceId(String resource, String resourceId, Pageable pageable) {
        log.info("Fetching logs for resource: {} / {}", resource, resourceId);
        return activityLogRepository.findByResourceAndResourceIdOrderByTimestampDesc(resource, resourceId, pageable)
                .map(this::mapToResponse);
    }
    
    /**
     * Recherche avancée avec filtres
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> searchLogs(
            Long adminId,
            String resource,
            String action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        
        log.info("Searching logs with filters - adminId: {}, resource: {}, action: {}", 
                 adminId, resource, action);
        
        return activityLogRepository.findWithFilters(
                adminId, resource, action, startDate, endDate, pageable
        ).map(this::mapToResponse);
    }
    
    /**
     * Mapper de ActivityLog vers ActivityLogResponse
     */
    private ActivityLogResponse mapToResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .adminId(log.getAdminId())
                .adminName(log.getAdminName())
                .action(log.getAction())
                .resource(log.getResource())
                .resourceId(log.getResourceId())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }
}
