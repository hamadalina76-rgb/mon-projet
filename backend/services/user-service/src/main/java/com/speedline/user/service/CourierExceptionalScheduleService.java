package com.speedline.user.service;

import com.speedline.user.dto.CourierExceptionalScheduleDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface CourierExceptionalScheduleService {

    Page<CourierExceptionalScheduleDTO> getAll(
            int page, int size, String search,
            String exceptionType, LocalDate dateFrom, LocalDate dateTo,
            Long courierId);

    List<CourierExceptionalScheduleDTO> getByCourierInRange(
            Long courierId, LocalDate from, LocalDate to);

    CourierExceptionalScheduleDTO.OverlapResult checkOverlap(
            Long courierId, LocalDate startDate, LocalDate endDate, Long excludeId);

    CourierExceptionalScheduleDTO create(CourierExceptionalScheduleDTO.CreateRequest req);

    CourierExceptionalScheduleDTO update(Long id, CourierExceptionalScheduleDTO.CreateRequest req);
}
