package com.speedline.user.service.impl;

import com.speedline.user.domain.Admin;
import com.speedline.user.domain.SettingsAuditLog;
import com.speedline.user.dto.SettingsAuditLogDTO;
import com.speedline.user.repository.AdminRepository;
import com.speedline.user.repository.SettingsAuditLogRepository;
import com.speedline.user.service.SettingsAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsAuditLogServiceImpl implements SettingsAuditLogService {

    private final SettingsAuditLogRepository repository;
    private final AdminRepository            adminRepository;

    @Override
    @Transactional
    public void log(String action, Long adminId, String details) {
        String adminName = resolveAdminName(adminId);
        SettingsAuditLog entry = SettingsAuditLog.builder()
                .action(action)
                .adminId(adminId)
                .adminName(adminName)
                .details(details)
                .build();
        repository.save(entry);
        log.info("[SettingsAudit] action={} admin={} details={}", action, adminName, details);
    }

    @Override
    @Transactional(readOnly = true)
    public SettingsAuditLogDTO.LogListResponse getRecentLogs() {
        return toResponse(repository.findTop50ByOrderByCreatedAtDesc());
    }

    @Override
    @Transactional(readOnly = true)
    public SettingsAuditLogDTO.PagedLogResponse getFilteredLogs(
            String action, String adminName, String date, int page, int size) {

        final String    actionParam    = StringUtils.hasText(action)    ? action.trim()         : null;
        final String    adminNameParam = StringUtils.hasText(adminName) ? adminName.trim()      : null;
        final LocalDate dateParam      = StringUtils.hasText(date)      ? LocalDate.parse(date) : null;

        Specification<SettingsAuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actionParam != null) {
                predicates.add(cb.equal(root.get("action"), actionParam));
            }
            if (adminNameParam != null) {
                predicates.add(cb.like(
                        cb.lower(root.get("adminName")),
                        "%" + adminNameParam.toLowerCase() + "%"
                ));
            }
            if (dateParam != null) {
                predicates.add(cb.between(
                        root.get("createdAt"),
                        dateParam.atStartOfDay(),
                        dateParam.plusDays(1).atStartOfDay()
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SettingsAuditLog> resultPage = repository.findAll(spec, pageable);

        List<SettingsAuditLogDTO.LogEntry> entries = resultPage.getContent().stream()
                .map(this::toEntry)
                .collect(Collectors.toList());

        return SettingsAuditLogDTO.PagedLogResponse.builder()
                .logs(entries)
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SettingsAuditLogDTO.LogListResponse toResponse(List<SettingsAuditLog> logs) {
        List<SettingsAuditLogDTO.LogEntry> entries = logs.stream()
                .map(this::toEntry)
                .collect(Collectors.toList());
        return SettingsAuditLogDTO.LogListResponse.builder().logs(entries).build();
    }

    private SettingsAuditLogDTO.LogEntry toEntry(SettingsAuditLog l) {
        return SettingsAuditLogDTO.LogEntry.builder()
                .id(l.getId())
                .action(l.getAction())
                .adminId(l.getAdminId())
                .adminName(l.getAdminName())
                .details(l.getDetails())
                .createdAt(l.getCreatedAt().toString())
                .build();
    }

    private String resolveAdminName(Long adminId) {
        if (adminId == null) return "Système";
        return adminRepository.findByUserId(adminId)
                .map(Admin::getFullName)
                .orElse("Admin #" + adminId);
    }
}
