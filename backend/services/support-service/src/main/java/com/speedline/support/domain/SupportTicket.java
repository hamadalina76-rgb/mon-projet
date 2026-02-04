package com.speedline.support.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité SupportTicket - Tickets de support
 */
@Entity
@Table(name = "support_tickets", indexes = {
    @Index(name = "idx_ticket_number", columnList = "ticket_number", unique = true),
    @Index(name = "idx_ticket_user", columnList = "user_id"),
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_assigned", columnList = "assigned_to")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @OneToMany(mappedBy = "ticketId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TicketMessage> messages = new ArrayList<>();

    @PrePersist
    public void generateTicketNumber() {
        if (this.ticketNumber == null) {
            String year = String.valueOf(java.time.Year.now().getValue());
            String random = String.format("%06d", (int) (Math.random() * 1000000));
            this.ticketNumber = "TKT-" + year + "-" + random;
        }
    }

    public enum UserType {
        CUSTOMER, COURIER, PARTNER
    }
}
