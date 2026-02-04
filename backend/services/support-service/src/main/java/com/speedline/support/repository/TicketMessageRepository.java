package com.speedline.support.repository;

import com.speedline.support.domain.TicketMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour TicketMessage
 */
@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    /**
     * Trouver les messages d'un ticket
     */
    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    /**
     * Trouver les messages d'un ticket avec pagination
     */
    Page<TicketMessage> findByTicketId(Long ticketId, Pageable pageable);

    /**
     * Trouver les messages d'un expéditeur
     */
    List<TicketMessage> findByTicketIdAndSenderId(Long ticketId, Long senderId);

    /**
     * Trouver le dernier message d'un ticket
     */
    @Query("SELECT m FROM TicketMessage m WHERE m.ticketId = :ticketId ORDER BY m.createdAt DESC")
    TicketMessage findLastMessageByTicket(@Param("ticketId") Long ticketId);

    /**
     * Compter les messages d'un ticket
     */
    long countByTicketId(Long ticketId);

    /**
     * Supprimer les messages d'un ticket
     */
    void deleteByTicketId(Long ticketId);

    /**
     * Trouver les messages non internes d'un ticket
     */
    @Query("SELECT m FROM TicketMessage m WHERE m.ticketId = :ticketId AND m.isInternal = false ORDER BY m.createdAt ASC")
    List<TicketMessage> findPublicMessagesByTicket(@Param("ticketId") Long ticketId);
}
