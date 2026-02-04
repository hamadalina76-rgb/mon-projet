package com.speedline.support.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité TicketMessage - Messages dans un ticket
 */
@Entity
@Table(name = "ticket_messages", indexes = {
    @Index(name = "idx_message_ticket", columnList = "ticket_id"),
    @Index(name = "idx_message_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType;

    @Column(name = "sender_name")
    private String senderName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * URLs des pièces jointes (JSON array)
     */
    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(name = "is_internal")
    @Builder.Default
    private Boolean isInternal = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum SenderType {
        USER, ADMIN, SYSTEM
    }
}
