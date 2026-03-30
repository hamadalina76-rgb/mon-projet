package com.speedline.user.service;

import com.speedline.user.dto.AppWorkingHoursDTO;

public interface AppWorkingHoursService {

    AppWorkingHoursDTO.WeeklyScheduleResponse getWeeklySchedule();

    AppWorkingHoursDTO.WeeklyScheduleResponse updateWeeklySchedule(
            AppWorkingHoursDTO.UpdateWeeklyScheduleRequest request, Long adminId);

    /** Returns live open/closed status for the current moment. */
    AppWorkingHoursDTO.TodayStatusResponse getTodayStatus();
}
