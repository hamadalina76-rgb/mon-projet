package com.speedline.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_internal_notes", indexes = {
    @Index(name = "idx_internal_note_order", columnList = "orderId"),
    @Index(name = "idx_internal_note_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInternalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String visibility = "ADMIN_ONLY";

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 100)
    private String authorName;

    @Column(length = 50)
    private String authorRole;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
