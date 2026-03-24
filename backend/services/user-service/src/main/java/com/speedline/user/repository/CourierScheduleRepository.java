package com.speedline.user.repository;

import com.speedline.user.domain.CourierSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourierScheduleRepository extends JpaRepository<CourierSchedule, Long> {
    /** Planning permanent (weekStartDate IS NULL) */
    Optional<CourierSchedule> findByCourierIdAndWeekStartDateIsNull(Long courierId);
    /** Planning pour une semaine spécifique */
    Optional<CourierSchedule> findByCourierIdAndWeekStartDate(Long courierId, java.time.LocalDate weekStart);
    List<CourierSchedule> findByCourierIdOrderByWeekStartDateDesc(Long courierId);
}
