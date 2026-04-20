package com.speedline.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneInternalCourierAssignmentDTO {
    private Long courierId;
    private Set<DayOfWeek> workDays;
    private LocalTime startTime;
    private LocalTime endTime;
}