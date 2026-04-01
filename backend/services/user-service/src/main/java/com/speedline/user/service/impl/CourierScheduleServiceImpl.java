package com.speedline.user.service.impl;

import com.speedline.user.domain.*;
import com.speedline.user.dto.CourierScheduleDTO;
import com.speedline.user.dto.ScheduleTemplateDTO;
import com.speedline.user.dto.ShiftDTO;
import com.speedline.user.exception.CourierNotFoundException;
import com.speedline.user.repository.CourierExceptionalScheduleRepository;
import com.speedline.user.repository.CourierRepository;
import com.speedline.user.repository.CourierScheduleRepository;
import com.speedline.user.repository.ScheduleTemplateRepository;
import com.speedline.user.service.CourierScheduleAuditLogService;
import com.speedline.user.service.CourierScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierScheduleServiceImpl implements CourierScheduleService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CourierScheduleRepository scheduleRepository;
    private final CourierExceptionalScheduleRepository exceptionalScheduleRepository;
    private final ScheduleTemplateRepository templateRepository;
    private final CourierRepository courierRepository;
    private final ScheduleTemplateServiceImpl templateServiceImpl;
    private final CourierScheduleAuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public CourierScheduleDTO.Response getSchedule(Long courierId) {
        return scheduleRepository.findByCourierIdAndWeekStartDateIsNull(courierId)
                .map(s -> toResponse(s, loadTemplateNames(List.of(s))))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierScheduleDTO.Response> getAllSchedules(Long courierId) {
        List<CourierSchedule> schedules =
                scheduleRepository.findByCourierIdOrderByWeekStartDateDesc(courierId);
        Map<Long, String> templateNames = loadTemplateNames(schedules);
        return schedules.stream()
                .map(s -> toResponse(s, templateNames))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourierScheduleDTO.Response saveSchedule(Long courierId, CourierScheduleDTO.SaveRequest req) {
        validateCourierIsInternal(courierId);
        if (req.getDays() != null) validateShifts(req.getDays());

        boolean isPermanent = req.getIsPermanent() == null || req.getIsPermanent();
        LocalDate weekStart = isPermanent ? null : req.getWeekStartDate();

        CourierSchedule schedule;
        Optional<CourierSchedule> existing = isPermanent
                ? scheduleRepository.findByCourierIdAndWeekStartDateIsNull(courierId)
                : (weekStart != null ? scheduleRepository.findByCourierIdAndWeekStartDate(courierId, weekStart) : Optional.empty());

        if (existing.isPresent()) {
            schedule = existing.get();
            schedule.getShifts().clear();
        } else {
            schedule = CourierSchedule.builder()
                    .courierId(courierId)
                    .weekStartDate(weekStart)
                    .isPermanent(isPermanent)
                    .isActive(true)
                    .build();
        }

        if (req.getTemplateId() != null) {
            schedule.setTemplateId(req.getTemplateId());
        }

        if (req.getDays() != null) {
            addShiftsFromMap(schedule, req.getDays());
        }

        boolean isUpdate = existing.isPresent();
        CourierSchedule saved = scheduleRepository.save(schedule);

        int dayCount = req.getDays() != null ? req.getDays().size() : 0;
        String details = isUpdate
                ? String.format("Planning mis à jour (%d jours renseignés, %s)",
                        dayCount, isPermanent ? "permanent" : "semaine du " + weekStart)
                : String.format("Nouveau planning créé (%d jours renseignés, %s)",
                        dayCount, isPermanent ? "permanent" : "semaine du " + weekStart);
        auditLogService.log(courierId, isUpdate ? "UPDATED" : "CREATED", details,
                saved.getId(), saved.getTemplateId(), null);

        return toResponse(saved, loadTemplateNames(List.of(saved)));
    }

    @Override
    @Transactional
    public CourierScheduleDTO.Response applyTemplate(Long courierId, Long templateId, LocalDate effectiveFrom) {
        validateCourierIsInternal(courierId);
        ScheduleTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template introuvable: " + templateId));

        LocalDate effectiveDate = effectiveFrom != null ? effectiveFrom : LocalDate.now().plusDays(1);
        LocalDate weekMonday = effectiveDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        Optional<CourierSchedule> existingWeekSchedule =
                scheduleRepository.findByCourierIdAndWeekStartDate(courierId, weekMonday);
        Optional<CourierSchedule> existingPermanentSchedule =
                scheduleRepository.findByCourierIdAndWeekStartDateIsNull(courierId);

        Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> previousWeekDays =
                existingWeekSchedule
                        .map(ws -> shiftsToInputMap(ws.getShifts()))
                        .orElseGet(() -> existingPermanentSchedule
                                .map(ps -> shiftsToInputMap(ps.getShifts()))
                                .orElseGet(this::emptyInputMap));

        Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> templateDays =
                templateShiftsToInputMap(template);

        CourierSchedule schedule = existingPermanentSchedule
                .orElse(CourierSchedule.builder().courierId(courierId).isPermanent(true).isActive(true).build());

        schedule.setTemplateId(templateId);
        schedule.getShifts().clear();
        addShiftsFromMap(schedule, templateDays);

        log.info("Template {} appliqué au livreur {} à partir du {}", templateId, courierId, effectiveDate);
        CourierSchedule saved = scheduleRepository.save(schedule);

        boolean hasDaysBeforeEffectiveDate = effectiveDate.isAfter(weekMonday);
        if (existingWeekSchedule.isPresent() || hasDaysBeforeEffectiveDate) {
            Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> transitionWeek = new LinkedHashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                LocalDate dateOfDay = weekMonday.plusDays(day.getValue() - 1L);
                boolean keepOld = dateOfDay.isBefore(effectiveDate);
                transitionWeek.put(
                        day,
                        cloneShiftInputs((keepOld ? previousWeekDays : templateDays).getOrDefault(day, List.of())));
            }

            CourierSchedule weekOverride = existingWeekSchedule.orElse(
                    CourierSchedule.builder()
                            .courierId(courierId)
                            .weekStartDate(weekMonday)
                            .isPermanent(false)
                            .isActive(true)
                            .build());

            weekOverride.setTemplateId(templateId);
            weekOverride.getShifts().clear();
            addShiftsFromMap(weekOverride, transitionWeek);
            scheduleRepository.save(weekOverride);
        }

        auditLogService.log(courierId, "TEMPLATE_APPLIED",
                "Template '" + template.getName() + "' appliqué (effectiveFrom=" + effectiveDate + ")",
                saved.getId(), templateId, template.getName());

        CourierScheduleDTO.Response response = toResponse(saved, loadTemplateNames(List.of(saved)));
        response.setTemplateEffectiveFrom(effectiveDate);
        return response;
    }

    @Override
    @Transactional
    public CourierScheduleDTO.Response copyDay(Long courierId, CourierScheduleDTO.CopyDayRequest req) {
        CourierSchedule schedule = scheduleRepository.findByCourierIdAndWeekStartDateIsNull(courierId)
                .orElseThrow(() -> new IllegalArgumentException("Aucun planning pour le livreur " + courierId));

        List<CourierScheduleShift> sourceShifts = schedule.getShifts().stream()
                .filter(s -> s.getDayOfWeek() == req.getSourceDay())
                .sorted(Comparator.comparingInt(CourierScheduleShift::getShiftOrder))
                .collect(Collectors.toList());

        // Remove target day shifts then add copies
        req.getTargetDays().forEach(targetDay -> {
            schedule.getShifts().removeIf(s -> s.getDayOfWeek() == targetDay);
            AtomicInteger order = new AtomicInteger(0);
            sourceShifts.forEach(src -> {
                CourierScheduleShift copy = CourierScheduleShift.builder()
                        .schedule(schedule)
                        .dayOfWeek(targetDay)
                        .shiftOrder(order.getAndIncrement())
                        .startTime(src.getStartTime())
                        .endTime(src.getEndTime())
                        .breakStart(src.getBreakStart())
                        .breakEnd(src.getBreakEnd())
                        .build();
                schedule.getShifts().add(copy);
            });
        });

        CourierSchedule saved = scheduleRepository.save(schedule);
        String targetStr = req.getTargetDays().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        auditLogService.log(courierId, "DAY_COPIED",
                req.getSourceDay().name() + " → " + targetStr,
                saved.getId(), null, null);
        return toResponse(saved, loadTemplateNames(List.of(saved)));
    }

    @Override
    @Transactional
    public void deleteSchedule(Long courierId, Long scheduleId) {
        scheduleRepository.deleteById(scheduleId);
        auditLogService.log(courierId, "DELETED", "Planning #" + scheduleId + " supprimé",
                scheduleId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CourierScheduleDTO.EffectiveScheduleResponse getEffectiveSchedule(Long courierId, LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        LocalDate weekMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 1. Chercher d'abord un planning spécifique pour cette semaine
        Optional<CourierSchedule> weeklySchedule = scheduleRepository
                .findByCourierIdAndWeekStartDate(courierId, weekMonday);

        // 2. Sinon, utiliser le planning permanent
        Optional<CourierSchedule> mainSchedule = weeklySchedule.isPresent() 
                ? weeklySchedule 
                : scheduleRepository.findByCourierIdAndWeekStartDateIsNull(courierId);

        // 3. Extraire les shifts réguliers (du planning hebdo ou permanent)
        List<ShiftDTO> dayShifts = mainSchedule
                .map(schedule -> schedule.getShifts().stream()
                        .filter(s -> s.getDayOfWeek() == dow)
                        .sorted(Comparator.comparing(CourierScheduleShift::getShiftOrder))
                        .map(s -> ShiftDTO.builder()
                                .id(s.getId())
                                .shiftOrder(s.getShiftOrder())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .breakStart(s.getBreakStart())
                                .breakEnd(s.getBreakEnd())
                                .build())
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());

        // 4. Vérifier s'il existe une exception active qui couvre cette date
        List<CourierExceptionalSchedule> exceptions =
                exceptionalScheduleRepository.findActiveOnDate(courierId, date);

        if (!exceptions.isEmpty()) {
            CourierExceptionalSchedule ex = exceptions.get(0);
            String status = Boolean.TRUE.equals(ex.getIsRestPeriod())
                    ? "EXCEPTION_REST" : "EXCEPTION_SPECIAL";
            
            return CourierScheduleDTO.EffectiveScheduleResponse.builder()
                    .status(status)
                    .date(date)
                    .dayOfWeek(dow)
                    .isRestDay(ex.getIsRestPeriod())
                    .shifts(dayShifts)
                    .exception(CourierScheduleDTO.ExceptionInfo.builder()
                            .id(ex.getId())
                            .exceptionType(ex.getExceptionType())
                            .label(ex.getLabel())
                            .startDate(ex.getStartDate())
                            .endDate(ex.getEndDate())
                            .reason(ex.getReason())
                            .isRestPeriod(ex.getIsRestPeriod())
                            .build())
                    .build();
        }

        // 5. Pas d'exception → status REGULAR
        boolean isRest = dayShifts.isEmpty();
        return CourierScheduleDTO.EffectiveScheduleResponse.builder()
                .status("REGULAR")
                .date(date)
                .dayOfWeek(dow)
                .isRestDay(isRest)
                .shifts(dayShifts)
                .exception(null)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Charge les noms des templates en un seul SELECT (anti N+1). */
    private Map<Long, String> loadTemplateNames(List<CourierSchedule> schedules) {
        Set<Long> ids = schedules.stream()
                .map(CourierSchedule::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return templateRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ScheduleTemplate::getId, ScheduleTemplate::getName));
    }

    private void validateCourierIsInternal(Long courierId) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> CourierNotFoundException.byId(courierId));
        if (courier.getCourierType() != CourierType.INTERNAL) {
            throw new IllegalArgumentException(
                    "Les plannings de travail sont réservés aux livreurs internes (INTERNAL)");
        }
    }

    private void validateShifts(Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> days) {
        days.forEach((day, shifts) -> {
            if (shifts == null || shifts.isEmpty()) return;
            shifts.forEach(s -> {
                if (s.getEndTime() != null && s.getStartTime() != null
                        && !s.getEndTime().isAfter(s.getStartTime())) {
                    throw new IllegalArgumentException(
                            "Plage invalide le " + day + ": l'heure de fin doit être après l'heure de début");
                }
            });
            List<ScheduleTemplateDTO.ShiftInput> sorted = shifts.stream()
                    .sorted(Comparator.comparing(ScheduleTemplateDTO.ShiftInput::getStartTime))
                    .collect(Collectors.toList());
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).getStartTime().isBefore(sorted.get(i - 1).getEndTime())) {
                    throw new IllegalArgumentException("Chevauchement de plages détecté le " + day);
                }
            }
        });
    }

    private void addShiftsFromMap(CourierSchedule schedule,
                                   Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> days) {
        days.forEach((day, shifts) -> {
            if (shifts == null) return;
            AtomicInteger order = new AtomicInteger(0);
            shifts.forEach(s -> {
                CourierScheduleShift shift = CourierScheduleShift.builder()
                        .schedule(schedule)
                        .dayOfWeek(day)
                        .shiftOrder(order.getAndIncrement())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .breakStart(s.getBreakStart())
                        .breakEnd(s.getBreakEnd())
                        .build();
                schedule.getShifts().add(shift);
            });
        });
    }

    private Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> emptyInputMap() {
        Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> map = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            map.put(day, new ArrayList<>());
        }
        return map;
    }

    private Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> templateShiftsToInputMap(ScheduleTemplate template) {
        Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> map = emptyInputMap();
        template.getShifts().stream()
                .sorted(Comparator.comparing(ScheduleTemplateShift::getDayOfWeek)
                        .thenComparing(ScheduleTemplateShift::getShiftOrder))
                .forEach(ts -> map.get(ts.getDayOfWeek()).add(
                        ScheduleTemplateDTO.ShiftInput.builder()
                                .startTime(ts.getStartTime())
                                .endTime(ts.getEndTime())
                                .breakStart(ts.getBreakStart())
                                .breakEnd(ts.getBreakEnd())
                                .build()));
        return map;
    }

    private Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> shiftsToInputMap(List<CourierScheduleShift> shifts) {
        Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> map = emptyInputMap();
        shifts.stream()
                .sorted(Comparator.comparing(CourierScheduleShift::getDayOfWeek)
                        .thenComparing(CourierScheduleShift::getShiftOrder))
                .forEach(s -> map.get(s.getDayOfWeek()).add(
                        ScheduleTemplateDTO.ShiftInput.builder()
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .breakStart(s.getBreakStart())
                                .breakEnd(s.getBreakEnd())
                                .build()));
        return map;
    }

    private List<ScheduleTemplateDTO.ShiftInput> cloneShiftInputs(List<ScheduleTemplateDTO.ShiftInput> shifts) {
        return shifts.stream()
                .map(s -> ScheduleTemplateDTO.ShiftInput.builder()
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .breakStart(s.getBreakStart())
                        .breakEnd(s.getBreakEnd())
                        .build())
                .collect(Collectors.toList());
    }

    private CourierScheduleDTO.Response toResponse(CourierSchedule cs, Map<Long, String> templateNames) {
        Map<DayOfWeek, List<ShiftDTO>> days = new LinkedHashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) days.put(d, new ArrayList<>());
        cs.getShifts().forEach(s -> days.get(s.getDayOfWeek()).add(
                ShiftDTO.builder()
                        .id(s.getId())
                        .shiftOrder(s.getShiftOrder())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .breakStart(s.getBreakStart())
                        .breakEnd(s.getBreakEnd())
                        .build()));

        String templateName = cs.getTemplateId() != null
                ? templateNames.get(cs.getTemplateId())
                : null;

        return CourierScheduleDTO.Response.builder()
                .id(cs.getId())
                .courierId(cs.getCourierId())
                .templateId(cs.getTemplateId())
                .templateName(templateName)
                .weekStartDate(cs.getWeekStartDate())
                .isPermanent(cs.getIsPermanent())
                .isActive(cs.getIsActive())
                .days(days)
                .createdAt(cs.getCreatedAt() != null ? cs.getCreatedAt().format(DT_FMT) : null)
                .build();
    }
}
