package com.speedline.user.service;

import com.speedline.user.domain.Customer.CustomerStatus;
import com.speedline.user.dto.CustomerDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service admin pour la gestion des clients (liste, filtres, actions, export, stats).
 */
public interface AdminCustomerService {

    /**
     * Liste paginée avec filtres (statut, recherche globale, dates).
     * search : recherche sur nom, prénom, email, téléphone, ville
     */
    Page<CustomerDTO> searchCustomers(
            CustomerStatus status,
            String search,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    );

    /**
     * Détail d'un client (délègue à CustomerService).
     */
    CustomerDTO getCustomerById(Long id);

    /**
     * Bloquer le compte (auth SUSPENDED + customer SUSPENDED).
     */
    void blockCustomer(Long customerId, Long adminId, String adminName);

    /**
     * Débloquer le compte (auth ACTIVE + customer ACTIVE).
     */
    void unblockCustomer(Long customerId, Long adminId, String adminName);

    /**
     * Supprimer le compte (auth delete + customer soft delete).
     */
    void deleteCustomer(Long customerId, Long adminId, String adminName);

    /**
     * Envoyer l'email de réinitialisation de mot de passe au client.
     */
    void sendResetPasswordEmail(Long customerId, Long adminId, String adminName);

    /**
     * Envoyer une notification (email) au client.
     */
    void sendNotification(Long customerId, String subject, String body, Long adminId, String adminName);

    /**
     * Export CSV ou Excel selon le format (mêmes filtres que la liste).
     */
    byte[] exportCustomers(String format, CustomerStatus status, String search,
                           LocalDateTime dateFrom, LocalDateTime dateTo);

    /**
     * Nombre de nouveaux clients par mois pour une année (clé "yyyy-MM", valeur count).
     */
    Map<String, Long> getNewCustomersByMonth(int year);
}
