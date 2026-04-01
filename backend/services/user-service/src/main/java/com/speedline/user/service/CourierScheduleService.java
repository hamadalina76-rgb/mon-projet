package com.speedline.user.service;

import com.speedline.user.dto.CourierScheduleDTO;

import java.time.LocalDate;
import java.util.List;

public interface CourierScheduleService {
    CourierScheduleDTO.Response getSchedule(Long courierId);
    List<CourierScheduleDTO.Response> getAllSchedules(Long courierId);
    CourierScheduleDTO.Response saveSchedule(Long courierId, CourierScheduleDTO.SaveRequest request);
    CourierScheduleDTO.Response applyTemplate(Long courierId, Long templateId, LocalDate effectiveFrom);
    CourierScheduleDTO.Response copyDay(Long courierId, CourierScheduleDTO.CopyDayRequest request);
    void deleteSchedule(Long courierId, Long scheduleId);

    /**
     * Retourne le planning effectif pour un livreur à une date donnée.
     * Si une exception active couvre cette date, elle est retournée (override).
     * Sinon, le planning hebdomadaire permanent est retourné.
     */
    CourierScheduleDTO.EffectiveScheduleResponse getEffectiveSchedule(Long courierId, LocalDate date);
}
