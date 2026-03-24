package com.speedline.user.service.impl;

import com.speedline.user.domain.ScheduleTemplate;
import com.speedline.user.domain.ScheduleTemplateShift;
import com.speedline.user.dto.ScheduleTemplateDTO;
import com.speedline.user.dto.ShiftDTO;
import com.speedline.user.repository.ScheduleTemplateRepository;
import com.speedline.user.service.ScheduleTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleTemplateServiceImpl implements ScheduleTemplateService {

    private final ScheduleTemplateRepository templateRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduleTemplateDTO.Response> getAllTemplates(Pageable pageable, String search, Boolean isActive) {
        String term = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<ScheduleTemplate> page;
        if (term != null && isActive != null) {
            page = templateRepository.findBySearchAndIsActive(term, isActive, pageable);
        } else if (term != null) {
            page = templateRepository.findBySearch(term, pageable);
        } else if (isActive != null) {
            page = templateRepository.findByIsActive(isActive, pageable);
        } else {
            page = templateRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Override
    public List<ScheduleTemplateDTO.Response> getActiveTemplates() {
        return templateRepository.findByIsActiveTrueOrderByNameAsc().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public ScheduleTemplateDTO.Response getTemplate(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public ScheduleTemplateDTO.Response createTemplate(ScheduleTemplateDTO.CreateRequest req) {
        validateShifts(req.getDays());
        ScheduleTemplate template = ScheduleTemplate.builder()
                .name(req.getName())
                .description(req.getDescription())
                .isActive(true)
                .build();
        addShiftsFromMap(template, req.getDays());
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public ScheduleTemplateDTO.Response updateTemplate(Long id, ScheduleTemplateDTO.CreateRequest req) {
        validateShifts(req.getDays());
        ScheduleTemplate template = findById(id);
        template.setName(req.getName());
        template.setDescription(req.getDescription());
        // Flush deletions BEFORE inserting new shifts to avoid UniqueConstraint violations
        // (Hibernate inserts before deleting by default)
        template.getShifts().clear();
        templateRepository.saveAndFlush(template);
        addShiftsFromMap(template, req.getDays());
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        templateRepository.delete(findById(id));
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        ScheduleTemplate t = findById(id);
        t.setIsActive(!Boolean.TRUE.equals(t.getIsActive()));
        templateRepository.save(t);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ScheduleTemplate findById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template introuvable: " + id));
    }

    private void addShiftsFromMap(ScheduleTemplate template,
                                   Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> days) {
        if (days == null) return;
        days.forEach((day, shifts) -> {
            if (shifts == null) return;
            AtomicInteger order = new AtomicInteger(0);
            shifts.forEach(s -> {
                ScheduleTemplateShift shift = ScheduleTemplateShift.builder()
                        .template(template)
                        .dayOfWeek(day)
                        .shiftOrder(order.getAndIncrement())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .breakStart(s.getBreakStart())
                        .breakEnd(s.getBreakEnd())
                        .build();
                template.getShifts().add(shift);
            });
        });
    }

    private void validateShifts(Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> days) {
        if (days == null) return;
        days.forEach((day, shifts) -> {
            if (shifts == null || shifts.isEmpty()) return;
            // Validate each shift: end > start
            shifts.forEach(s -> {
                if (s.getEndTime() != null && s.getStartTime() != null
                        && !s.getEndTime().isAfter(s.getStartTime())) {
                    throw new IllegalArgumentException(
                            "Plage invalide le " + day + ": l'heure de fin doit être après l'heure de début");
                }
            });
            // Validate no overlap
            List<ScheduleTemplateDTO.ShiftInput> sorted = shifts.stream()
                    .sorted(Comparator.comparing(ScheduleTemplateDTO.ShiftInput::getStartTime))
                    .collect(Collectors.toList());
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).getStartTime().isBefore(sorted.get(i - 1).getEndTime())) {
                    throw new IllegalArgumentException(
                            "Chevauchement de plages détecté le " + day);
                }
            }
        });
    }

    ScheduleTemplateDTO.Response toResponse(ScheduleTemplate t) {
        Map<DayOfWeek, List<ShiftDTO>> days = new LinkedHashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            days.put(d, new ArrayList<>());
        }
        t.getShifts().forEach(s -> days.get(s.getDayOfWeek()).add(
                ShiftDTO.builder()
                        .id(s.getId())
                        .shiftOrder(s.getShiftOrder())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .breakStart(s.getBreakStart())
                        .breakEnd(s.getBreakEnd())
                        .build()));
        return ScheduleTemplateDTO.Response.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .isActive(t.getIsActive())
                .days(days)
                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                .build();
    }
}
