package com.speedline.support.service;

import com.speedline.support.domain.TicketCategory;
import com.speedline.support.domain.TicketPriority;
import com.speedline.support.domain.TicketStatus;
import com.speedline.support.domain.SupportTicket.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service pour la gestion des tickets de support
 */
public interface SupportTicketService {

    /**
     * Créer un ticket
     */
    TicketDTO createTicket(Long userId, UserType userType, TicketCategory category,
                           TicketPriority priority, String subject, String description, Long orderId);

    /**
     * Récupérer un ticket par ID
     */
    TicketDTO getTicketById(Long ticketId);

    /**
     * Récupérer un ticket par numéro
     */
    TicketDTO getTicketByNumber(String ticketNumber);

    /**
     * Récupérer les tickets d'un utilisateur
     */
    Page<TicketDTO> getUserTickets(Long userId, Pageable pageable);

    /**
     * Récupérer les tickets par statut (admin)
     */
    Page<TicketDTO> getTicketsByStatus(TicketStatus status, Pageable pageable);

    /**
     * Assigner un ticket à un agent
     */
    TicketDTO assignTicket(Long ticketId, Long agentId, String agentName);

    /**
     * Mettre à jour le statut d'un ticket
     */
    TicketDTO updateStatus(Long ticketId, TicketStatus status);

    /**
     * Ajouter un message à un ticket
     */
    MessageDTO addMessage(Long ticketId, Long senderId, String senderType, String senderName, 
                          String message, List<String> attachments);

    /**
     * Récupérer les messages d'un ticket
     */
    List<MessageDTO> getTicketMessages(Long ticketId);

    /**
     * Résoudre un ticket
     */
    TicketDTO resolveTicket(Long ticketId, String resolution);

    /**
     * Rouvrir un ticket
     */
    TicketDTO reopenTicket(Long ticketId);

    /**
     * DTO pour les tickets
     */
    record TicketDTO(
            Long id,
            String ticketNumber,
            Long userId,
            UserType userType,
            Long orderId,
            TicketCategory category,
            TicketPriority priority,
            TicketStatus status,
            String subject,
            String description,
            Long assignedTo,
            String assignedToName,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt,
            java.time.LocalDateTime resolvedAt,
            Integer messageCount
    ) {}

    /**
     * DTO pour les messages
     */
    record MessageDTO(
            Long id,
            Long ticketId,
            Long senderId,
            String senderType,
            String senderName,
            String message,
            List<String> attachments,
            Boolean isInternal,
            java.time.LocalDateTime createdAt
    ) {}
}
