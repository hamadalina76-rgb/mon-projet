package com.speedline.user.service.impl;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.domain.Admin;
import com.speedline.user.domain.CourierExceptionalSchedule;
import com.speedline.user.dto.CourierExceptionalScheduleDTO;
import com.speedline.user.repository.AdminRepository;
import com.speedline.user.repository.CourierExceptionalScheduleRepository;
import com.speedline.user.repository.CourierRepository;
import com.speedline.user.service.CourierExceptionalScheduleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierExceptionalScheduleServiceImpl implements CourierExceptionalScheduleService {

    private final CourierExceptionalScheduleRepository repository;
    private final AdminRepository                      adminRepository;
    private final CourierRepository                    courierRepository;
    private final AuthServiceClient                    authServiceClient;

    // ── getAll ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<CourierExceptionalScheduleDTO> getAll(
            int page, int size,
            String search, String exceptionType,
            LocalDate dateFrom, LocalDate dateTo,
            Long courierId) {

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "startDate"));

        return repository.findAll(
                buildSpec(search, exceptionType, dateFrom, dateTo, courierId),
                pageable
        ).map(this::toDTO);
    }

    private Specification<CourierExceptionalSchedule> buildSpec(
            String search, String exceptionType,
            LocalDate dateFrom, LocalDate dateTo, Long courierId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("label")),       like),
                        cb.like(cb.lower(root.get("courierName")), like),
                        cb.like(cb.lower(root.get("reason")),      like)
                ));
            }
            if (exceptionType != null && !exceptionType.isBlank()) {
                predicates.add(cb.equal(root.get("exceptionType"), exceptionType));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), dateTo));
            }
            if (courierId != null) {
                predicates.add(cb.equal(root.get("courierId"), courierId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── getByCourierInRange ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CourierExceptionalScheduleDTO> getByCourierInRange(
            Long courierId, LocalDate from, LocalDate to) {
        return repository.findActiveInRange(courierId, from, to)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── checkOverlap ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CourierExceptionalScheduleDTO.OverlapResult checkOverlap(
            Long courierId, LocalDate startDate, LocalDate endDate, Long excludeId) {
        List<CourierExceptionalScheduleDTO> overlapping = repository
                .findOverlapping(courierId, startDate, endDate, excludeId)
                .stream().map(this::toDTO).collect(Collectors.toList());
        return CourierExceptionalScheduleDTO.OverlapResult.builder()
                .hasOverlap(!overlapping.isEmpty())
                .overlapping(overlapping)
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CourierExceptionalScheduleDTO create(CourierExceptionalScheduleDTO.CreateRequest req) {
        String courierName = resolveCourierName(req.getCourierId());
        Long   adminId     = resolveCurrentAdminUserId();
        String adminName   = resolveAdminName(adminId);

        CourierExceptionalSchedule entity = CourierExceptionalSchedule.builder()
                .courierId(req.getCourierId())
                .courierName(courierName)
                .exceptionType(req.getExceptionType())
                .label(req.getLabel())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .reason(req.getReason())
                .isRestPeriod(req.getIsRestPeriod() != null ? req.getIsRestPeriod() : true)
                .isActive(true)
                .adminId(adminId)
                .adminName(adminName)
                .build();

        return toDTO(repository.save(entity));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CourierExceptionalScheduleDTO update(Long id, CourierExceptionalScheduleDTO.CreateRequest req) {
        CourierExceptionalSchedule entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Planning exceptionnel non trouvé avec l'ID: " + id));

        entity.setCourierId(req.getCourierId());
        entity.setCourierName(resolveCourierName(req.getCourierId()));
        entity.setExceptionType(req.getExceptionType());
        entity.setLabel(req.getLabel());
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        entity.setReason(req.getReason());
        if (req.getIsRestPeriod() != null) entity.setIsRestPeriod(req.getIsRestPeriod());

        return toDTO(repository.save(entity));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String resolveCourierName(Long courierId) {
        try {
            var userInfo = authServiceClient.getUserById(
                courierRepository.findById(courierId)
                    .map(c -> c.getUserId())
                    .orElse(null)
            );
            if (userInfo != null) {
                String name = ((userInfo.getFirstName() != null ? userInfo.getFirstName() : "") + " "
                             + (userInfo.getLastName()  != null ? userInfo.getLastName()  : "")).trim();
                if (!name.isEmpty()) return name;
            }
        } catch (Exception e) {
            log.warn("Cannot resolve courier name for id={}: {}", courierId, e.getMessage());
        }
        return "Livreur #" + courierId;
    }

    private Long resolveCurrentAdminUserId() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sa) {
                String header = sa.getRequest().getHeader("X-User-Id");
                if (header != null && !header.isBlank()) return Long.parseLong(header.trim());
            }
        } catch (Exception e) {
            log.warn("Cannot resolve admin user: {}", e.getMessage());
        }
        return null;
    }

    private String resolveAdminName(Long adminUserId) {
        if (adminUserId == null) return "Système";
        return adminRepository.findByUserId(adminUserId)
                .map(Admin::getFullName)
                .orElse("Admin #" + adminUserId);
    }

    private CourierExceptionalScheduleDTO toDTO(CourierExceptionalSchedule e) {
        return CourierExceptionalScheduleDTO.builder()
                .id(e.getId())
                .courierId(e.getCourierId())
                .courierName(e.getCourierName())
                .exceptionType(e.getExceptionType())
                .label(e.getLabel())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .reason(e.getReason())
                .isRestPeriod(e.getIsRestPeriod())
                .isActive(e.getIsActive())
                .adminName(e.getAdminName())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
