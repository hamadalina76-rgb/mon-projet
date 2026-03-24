package com.speedline.user.service;

import com.speedline.user.dto.CourierScheduleDTO;

import java.util.List;

public interface CourierScheduleService {
    CourierScheduleDTO.Response getSchedule(Long courierId);
    List<CourierScheduleDTO.Response> getAllSchedules(Long courierId);
    CourierScheduleDTO.Response saveSchedule(Long courierId, CourierScheduleDTO.SaveRequest request);
    CourierScheduleDTO.Response applyTemplate(Long courierId, Long templateId);
    CourierScheduleDTO.Response copyDay(Long courierId, CourierScheduleDTO.CopyDayRequest request);
    void deleteSchedule(Long courierId, Long scheduleId);
}
