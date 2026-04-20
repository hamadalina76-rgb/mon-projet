package com.speedline.location.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "adm_zone_internal_courier_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_zone_internal_courier_zone_courier",
                        columnNames = {"zone_id", "courier_id"}
                )
        },
        indexes = {
                @Index(name = "idx_zone_internal_courier_zone", columnList = "zone_id"),
                @Index(name = "idx_zone_internal_courier_courier", columnList = "courier_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneInternalCourierAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "adm_zone_internal_courier_work_days",
            joinColumns = @JoinColumn(name = "assignment_id")
    )
    @Column(name = "work_day", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DayOfWeek> workDays = new HashSet<>();

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;
}