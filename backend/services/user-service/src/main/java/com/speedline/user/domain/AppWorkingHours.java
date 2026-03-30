package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_working_hours")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppWorkingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, unique = true, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
