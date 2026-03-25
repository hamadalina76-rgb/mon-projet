package com.speedline.user.service.impl;

import com.speedline.user.domain.Admin;
import com.speedline.user.domain.CourierChangeLog;
import com.speedline.user.dto.CourierChangeLogDTO;
import com.speedline.user.repository.AdminRepository;
import com.speedline.user.repository.CourierChangeLogRepository;
import com.speedline.user.service.CourierChangeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierChangeLogServiceImpl implements CourierChangeLogService {

    private final CourierChangeLogRepository repository;
    private final AdminRepository            adminRepository;

    @Override
    @Async
    public void log(String action, Long courierId,
                    String statusBefore, String statusAfter,
                    String courierTypeBefore, String courierTypeAfter,
                    String zoneIdsBefore, String zoneIdsAfter,
                    String description, String reason) {
        try {
            Long adminId   = resolveCurrentAdminUserId();
            String adminName = resolveAdminName(adminId);

            CourierChangeLog entry = CourierChangeLog.builder()
                    .courierId(courierId)
                    .adminId(adminId)
                    .adminName(adminName)
                    .action(action)
                    .statusBefore(statusBefore)
                    .statusAfter(statusAfter)
                    .courierTypeBefore(courierTypeBefore)
                    .courierTypeAfter(courierTypeAfter)
                    .zoneIdsBefore(zoneIdsBefore)
                    .zoneIdsAfter(zoneIdsAfter)
                    .description(description)
                    .reason(reason)
                    .build();

            repository.save(entry);
            log.info("✅ CourierChangeLog: admin={} action={} courierId={}", adminId, action, courierId);
        } catch (Exception e) {
            log.error("❌ Erreur CourierChangeLog: {}", e.getMessage());
        }
    }

    @Override
    public Page<CourierChangeLogDTO> getChangeLogs(Long courierId, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "changedAt"));
        return repository.findByCourierIdOrderByChangedAtDesc(courierId, pageable)
                .map(this::toDTO);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
                .orElse("Admin");
    }

    private CourierChangeLogDTO toDTO(CourierChangeLog e) {
        return CourierChangeLogDTO.builder()
                .id(e.getId())
                .courierId(e.getCourierId())
                .adminId(e.getAdminId())
                .adminName(e.getAdminName())
                .action(e.getAction())
                .statusBefore(e.getStatusBefore())
                .statusAfter(e.getStatusAfter())
                .courierTypeBefore(e.getCourierTypeBefore())
                .courierTypeAfter(e.getCourierTypeAfter())
                .zoneIdsBefore(e.getZoneIdsBefore())
                .zoneIdsAfter(e.getZoneIdsAfter())
                .description(e.getDescription())
                .reason(e.getReason())
                .changedAt(e.getChangedAt())
                .build();
    }
}
