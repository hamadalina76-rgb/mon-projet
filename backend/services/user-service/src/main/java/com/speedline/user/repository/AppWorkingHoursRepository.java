package com.speedline.user.repository;

import com.speedline.user.domain.AppWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface AppWorkingHoursRepository extends JpaRepository<AppWorkingHours, Long> {

    Optional<AppWorkingHours> findByDayOfWeek(DayOfWeek dayOfWeek);

    List<AppWorkingHours> findAllByOrderByDayOfWeekAsc();
}
