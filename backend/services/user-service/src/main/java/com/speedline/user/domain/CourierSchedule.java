package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courier_schedules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"courier_id", "week_start_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    /** Template source (nullable si schedule manuel) */
    @Column(name = "template_id")
    private Long templateId;

    /** NULL = planning permanent (défaut), non-null = semaine spécifique */
    @Column(name = "week_start_date")
    private LocalDate weekStartDate;

    @Builder.Default
    @Column(name = "is_permanent")
    private Boolean isPermanent = true;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("dayOfWeek ASC, shiftOrder ASC")
    @Builder.Default
    private List<CourierScheduleShift> shifts = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
