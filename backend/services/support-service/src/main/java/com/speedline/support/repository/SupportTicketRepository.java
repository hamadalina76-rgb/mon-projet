package com.speedline.support.repository;

import com.speedline.support.domain.SupportTicket;
import com.speedline.support.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour SupportTicket
 */
@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    /**
     * Trouver un ticket par numéro
     */
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    /**
     * Trouver les tickets d'un utilisateur
     */
    Page<SupportTicket> findByUserId(Long userId, Pageable pageable);

    /**
     * Trouver les tickets par statut
     */
    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

    /**
     * Trouver les tickets assignés à un agent
     */
    Page<SupportTicket> findByAssignedTo(Long assignedTo, Pageable pageable);

    /**
     * Trouver les tickets non assignés
     */
    List<SupportTicket> findByAssignedToIsNullAndStatusNot(TicketStatus status);

    /**
     * Trouver les tickets par catégorie
     */
    Page<SupportTicket> findByCategory(com.speedline.support.domain.TicketCategory category, Pageable pageable);

    /**
     * Trouver les tickets par priorité
     */
    Page<SupportTicket> findByPriority(com.speedline.support.domain.TicketPriority priority, Pageable pageable);

    /**
     * Trouver les tickets ouverts
     */
    List<SupportTicket> findByStatusIn(List<TicketStatus> statuses);

    /**
     * Assigner un ticket à un agent
     */
    @Modifying
    @Query("UPDATE SupportTicket t SET t.assignedTo = :agentId, t.assignedToName = :agentName WHERE t.id = :ticketId")
    int assignTicket(@Param("ticketId") Long ticketId, 
                     @Param("agentId") Long agentId,
                     @Param("agentName") String agentName);

    /**
     * Mettre à jour le statut
     */
    @Modifying
    @Query("UPDATE SupportTicket t SET t.status = :status WHERE t.id = :ticketId")
    int updateStatus(@Param("ticketId") Long ticketId, @Param("status") TicketStatus status);

    /**
     * Marquer comme résolu
     */
    @Modifying
    @Query("UPDATE SupportTicket t SET t.status = 'RESOLVED', t.resolvedAt = CURRENT_TIMESTAMP WHERE t.id = :ticketId")
    int markAsResolved(@Param("ticketId") Long ticketId);

    /**
     * Compter les tickets par statut
     */
    long countByStatus(TicketStatus status);

    /**
     * Compter les tickets non assignés
     */
    long countByAssignedToIsNullAndStatusNot(TicketStatus status);
}
