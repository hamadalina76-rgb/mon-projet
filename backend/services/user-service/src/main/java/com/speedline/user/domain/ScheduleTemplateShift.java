package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_template_shifts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"template_id", "day_of_week", "shift_order"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "template")
@ToString(exclude = "template")
public class ScheduleTemplateShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ScheduleTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "shift_order", nullable = false)
    private Integer shiftOrder;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Début de la pause (null = pas de pause) */
    @Column(name = "break_start")
    private LocalTime breakStart;

    /** Fin de la pause */
    @Column(name = "break_end")
    private LocalTime breakEnd;
}
