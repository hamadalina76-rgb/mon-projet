package com.speedline.user.service.impl;

import com.speedline.user.domain.AppWorkingHours;
import com.speedline.user.dto.AppWorkingHoursDTO;
import com.speedline.user.repository.AppWorkingHoursRepository;
import com.speedline.user.service.AppWorkingHoursService;
import com.speedline.user.service.SettingsAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppWorkingHoursServiceImpl implements AppWorkingHoursService {

    private final AppWorkingHoursRepository repository;
    private final SettingsAuditLogService    auditLogService;

    /** Canonical order: Mon → Sun */
    private static final List<DayOfWeek> WEEK_ORDER = Arrays.asList(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    );

    // ── Read ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AppWorkingHoursDTO.WeeklyScheduleResponse getWeeklySchedule() {
        Map<DayOfWeek, AppWorkingHours> byDay = repository.findAll().stream()
                .collect(Collectors.toMap(AppWorkingHours::getDayOfWeek, h -> h));

        List<AppWorkingHoursDTO.DaySchedule> days = WEEK_ORDER.stream()
                .map(dow -> {
                    AppWorkingHours h = byDay.getOrDefault(dow,
                            AppWorkingHours.builder()
                                    .dayOfWeek(dow)
                                    .isOpen(false)
                                    .build());
                    return toDTO(h);
                })
                .collect(Collectors.toList());

        return AppWorkingHoursDTO.WeeklyScheduleResponse.builder()
                .days(days)
                .build();
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AppWorkingHoursDTO.WeeklyScheduleResponse updateWeeklySchedule(
            AppWorkingHoursDTO.UpdateWeeklyScheduleRequest request, Long adminId) {

        for (AppWorkingHoursDTO.DaySchedule dto : request.getDays()) {
            DayOfWeek dow = DayOfWeek.valueOf(dto.getDayOfWeek().toUpperCase());
            AppWorkingHours entity = repository.findByDayOfWeek(dow)
                    .orElseGet(() -> AppWorkingHours.builder().dayOfWeek(dow).build());

            entity.setIsOpen(dto.isOpen());
            entity.setOpenTime(dto.isOpen() && dto.getOpenTime() != null
                    ? LocalTime.parse(dto.getOpenTime()) : null);
            entity.setCloseTime(dto.isOpen() && dto.getCloseTime() != null
                    ? LocalTime.parse(dto.getCloseTime()) : null);

            repository.save(entity);
        }

        // Build readable details
        StringBuilder sb = new StringBuilder();
        for (AppWorkingHoursDTO.DaySchedule d : request.getDays()) {
            if (d.isOpen()) {
                sb.append(d.getDayOfWeek()).append(": ").append(d.getOpenTime())
                  .append(" - ").append(d.getCloseTime()).append(" | ");
            } else {
                sb.append(d.getDayOfWeek()).append(": Fermé | ");
            }
        }
        auditLogService.log("WORKING_HOURS_UPDATED", adminId, sb.toString().replaceAll(" \\| $", ""));

        log.info("App working hours updated");
        return getWeeklySchedule();
    }

    // ── Mapper ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AppWorkingHoursDTO.TodayStatusResponse getTodayStatus() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now   = LocalTime.now();

        return repository.findByDayOfWeek(today)
                .map(h -> {
                    boolean open = Boolean.TRUE.equals(h.getIsOpen())
                            && h.getOpenTime() != null
                            && h.getCloseTime() != null
                            && !now.isBefore(h.getOpenTime())
                            && now.isBefore(h.getCloseTime());
                    String openStr  = h.getOpenTime()  != null ? h.getOpenTime().toString().substring(0, 5)  : null;
                    String closeStr = h.getCloseTime() != null ? h.getCloseTime().toString().substring(0, 5) : null;
                    return AppWorkingHoursDTO.TodayStatusResponse.builder()
                            .currentDay(today.name())
                            .todayIsOpen(Boolean.TRUE.equals(h.getIsOpen()))
                            .openTime(openStr)
                            .closeTime(closeStr)
                            .currentlyOpen(open)
                            .build();
                })
                .orElseGet(() -> AppWorkingHoursDTO.TodayStatusResponse.builder()
                        .currentDay(today.name())
                        .todayIsOpen(false)
                        .currentlyOpen(false)
                        .build());
    }

    // ── Mapper ────────────────────────────────────────────────────────────

    private AppWorkingHoursDTO.DaySchedule toDTO(AppWorkingHours h) {
        return AppWorkingHoursDTO.DaySchedule.builder()
                .dayOfWeek(h.getDayOfWeek().name())
                .isOpen(Boolean.TRUE.equals(h.getIsOpen()))
                .openTime(h.getOpenTime() != null
                        ? h.getOpenTime().toString().substring(0, 5) : null)
                .closeTime(h.getCloseTime() != null
                        ? h.getCloseTime().toString().substring(0, 5) : null)
                .build();
    }
}
