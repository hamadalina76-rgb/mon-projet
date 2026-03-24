package com.speedline.user.service.impl;

import com.speedline.user.domain.Admin;
import com.speedline.user.domain.CourierScheduleAuditLog;
import com.speedline.user.dto.CourierScheduleAuditLogDTO;
import com.speedline.user.repository.AdminRepository;
import com.speedline.user.repository.CourierScheduleAuditLogRepository;
import com.speedline.user.service.CourierScheduleAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierScheduleAuditLogServiceImpl implements CourierScheduleAuditLogService {

    private static final DateTimeFormatter DT_FMT       = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime     LOG_MIN      = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime     LOG_MAX      = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

    private final CourierScheduleAuditLogRepository auditLogRepository;
    private final AdminRepository adminRepository;

    @Override
    @Transactional
    public void log(Long courierId, String action, String details,
                    Long scheduleId, Long templateId, String templateName) {
        Long adminUserId = resolveCurrentAdminUserId();
        String adminName = resolveAdminName(adminUserId);

        CourierScheduleAuditLog entry = CourierScheduleAuditLog.builder()
                .courierId(courierId)
                .scheduleId(scheduleId)
                .action(action)
                .templateId(templateId)
                .templateName(templateName)
                .details(details)
                .adminId(adminUserId)
                .adminName(adminName)
                .build();

        auditLogRepository.save(entry);
        log.debug("Audit [courier={}] action={} by {}", courierId, action, adminName);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierScheduleAuditLogDTO.Response> getAuditLogs(
            Long courierId, int page, int size, String action, String dateFrom, String dateTo) {

        LocalDateTime from = (dateFrom != null && !dateFrom.isBlank())
                ? LocalDate.parse(dateFrom).atStartOfDay() : LOG_MIN;
        LocalDateTime to   = (dateTo   != null && !dateTo.isBlank())
                ? LocalDate.parse(dateTo).atTime(23, 59, 59) : LOG_MAX;
        String actionFilter = (action != null && !action.isBlank()) ? action : null;

        PageRequest pageable = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return auditLogRepository
                .findWithFilters(courierId, actionFilter, from, to, pageable)
                .map(this::toResponse);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Long resolveCurrentAdminUserId() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                String header = servletAttrs.getRequest().getHeader("X-User-Id");
                if (header != null && !header.isBlank()) {
                    return Long.parseLong(header.trim());
                }
            }
        } catch (Exception e) {
            log.warn("Cannot resolve admin user from request context: {}", e.getMessage());
        }
        return null;
    }

    private String resolveAdminName(Long adminUserId) {
        if (adminUserId == null) return "Système";
        return adminRepository.findByUserId(adminUserId)
                .map(Admin::getFullName)
                .orElse("Admin #" + adminUserId);
    }

    private CourierScheduleAuditLogDTO.Response toResponse(CourierScheduleAuditLog entry) {
        return new CourierScheduleAuditLogDTO.Response(
                entry.getId(),
                entry.getCourierId(),
                entry.getScheduleId(),
                entry.getAction(),
                entry.getTemplateId(),
                entry.getTemplateName(),
                entry.getDetails(),
                entry.getAdminId(),
                entry.getAdminName(),
                entry.getCreatedAt() != null ? entry.getCreatedAt().format(DT_FMT) : null
        );
    }
}
