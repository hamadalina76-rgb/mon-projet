package com.speedline.order.service;

import com.speedline.order.domain.OrderStatus;
import com.speedline.order.dto.AdminLogDTO;
import com.speedline.order.dto.OrderStatsResponse;
import com.speedline.order.dto.CreateOrderRequest;
import com.speedline.order.dto.OrderResponse;
import com.speedline.order.dto.PartnerOrderHistorySummaryDTO;
import com.speedline.order.dto.checkout.CheckoutOrderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service principal pour la gestion des commandes
 * 
 * Ce service gère toutes les opérations CRUD et de workflow des commandes :
 * - Création et validation de commandes
 * - Changement de statut
 * - Recherche et filtrage
 * - Assignation de livreur
 */
public interface OrderService {

    // ==================== CRÉATION ====================

    /**
     * Créer une nouvelle commande
     * 
     * @param request CreateOrderRequest contenant:
     *                - customerId (Long, obligatoire): ID du client
     *                - partnerId (Long, obligatoire): ID du partenaire
     *                - items (List, obligatoire): Articles de la commande
     *                - deliveryAddressId (Long): ID de l'adresse de livraison
     *                - type (OrderType): DELIVERY ou PICKUP
     *                - paymentMethod (PaymentMethod): CASH, CARD, WALLET
     *                - promoCode (String, optionnel): Code promo
     *                - tip (BigDecimal, optionnel): Pourboire
     *                - deliveryInstructions, customerNotes (String, optionnel)
     * @return OrderResponse avec:
     *         - id, orderNumber: Identifiants de la commande
     *         - status: PENDING (en attente de paiement) ou CONFIRMED (si paiement ok)
     *         - total: Montant total calculé
     *         - estimatedDeliveryTime: Heure estimée de livraison
     * @throws CustomerNotFoundException si le client n'existe pas
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     * @throws PartnerNotAvailableException si le partenaire n'accepte pas de commandes
     * @throws ProductNotAvailableException si un produit n'est pas disponible
     * @throws InvalidPromoCodeException si le code promo est invalide
     * @throws MinimumOrderNotMetException si le minimum de commande n'est pas atteint
     * @throws AddressNotInDeliveryZoneException si l'adresse n'est pas dans la zone de livraison
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Créer une commande à partir du payload checkout mobile (cartItems + paymentMethod).
     */
    OrderResponse createOrderFromCheckout(Long customerId, String customerName, CheckoutOrderRequest request);

    // ==================== LECTURE ====================

    /**
     * Récupérer une commande par son ID
     * 
     * @param orderId ID de la commande
     * @return OrderResponse complet avec articles et historique
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    OrderResponse getOrderById(Long orderId);

    /**
     * Récupérer une commande par son numéro
     * 
     * @param orderNumber Numéro de commande (ex: "ORD-2026-00001")
     * @return OrderResponse complet
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    OrderResponse getOrderByNumber(String orderNumber);

    // ==================== MISE À JOUR DU STATUT ====================

    /**
     * Mettre à jour le statut d'une commande
     * 
     * @param orderId ID de la commande
     * @param newStatus Nouveau statut
     * @param actorType Type de l'acteur (CUSTOMER, PARTNER, COURIER, ADMIN, SYSTEM)
     * @param actorId ID de l'acteur (peut être null pour SYSTEM)
     * @param notes Notes optionnelles
     * @return OrderResponse mis à jour
     * @throws OrderNotFoundException si la commande n'existe pas
     * @throws InvalidStatusTransitionException si la transition de statut est invalide
     */
    OrderResponse updateStatus(Long orderId, OrderStatus newStatus, 
                                String actorType, Long actorId, String notes);

    /**
     * Confirmer une commande (partenaire)
     * PENDING/CONFIRMED -> PREPARING
     * 
     * @param orderId ID de la commande
     * @param partnerId ID du partenaire
     * @param estimatedPrepTime Temps de préparation estimé (minutes)
     * @return OrderResponse avec status=PREPARING
     * @throws OrderNotFoundException si la commande n'existe pas
     * @throws UnauthorizedException si le partenaire n'est pas le bon
     */
    OrderResponse confirmOrder(Long orderId, Long partnerId, Integer estimatedPrepTime);

    /**
     * Marquer une commande comme prête (partenaire)
     * PREPARING -> READY_FOR_PICKUP
     * 
     * @param orderId ID de la commande
     * @param partnerId ID du partenaire
     * @return OrderResponse avec status=READY_FOR_PICKUP
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    OrderResponse markAsReady(Long orderId, Long partnerId);

    /**
     * Annuler une commande
     * 
     * @param orderId ID de la commande
     * @param cancelledBy Qui annule (CUSTOMER, PARTNER, COURIER, ADMIN)
     * @param actorId ID de l'acteur
     * @param reason Raison de l'annulation
     * @return OrderResponse avec status=CANCELLED
     * @throws OrderNotFoundException si la commande n'existe pas
     * @throws OrderNotCancellableException si la commande ne peut pas être annulée
     */
    OrderResponse cancelOrder(Long orderId, String cancelledBy, Long actorId, String reason);

    // ==================== ASSIGNATION LIVREUR ====================

    /**
     * Assigner un livreur à une commande
     * 
     * @param orderId ID de la commande
     * @param courierId ID du livreur
     * @return OrderResponse avec livreur assigné et status=PICKED_UP ou IN_DELIVERY
     * @throws OrderNotFoundException si la commande n'existe pas
     * @throws CourierNotFoundException si le livreur n'existe pas
     * @throws CourierNotAvailableException si le livreur n'est pas disponible
     */
    OrderResponse assignCourier(Long orderId, Long courierId);

    /**
     * Marquer la commande comme récupérée par le livreur
     * READY_FOR_PICKUP -> PICKED_UP
     * 
     * @param orderId ID de la commande
     * @param courierId ID du livreur
     * @return OrderResponse avec status=PICKED_UP
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    OrderResponse markAsPickedUp(Long orderId, Long courierId);

    /**
     * Marquer la commande comme en cours de livraison
     * PICKED_UP -> IN_DELIVERY
     * 
     * @param orderId ID de la commande
     * @param courierId ID du livreur
     * @return OrderResponse avec status=IN_DELIVERY
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    OrderResponse markAsInDelivery(Long orderId, Long courierId);

    /**
     * Marquer la commande comme livrée
     * IN_DELIVERY -> DELIVERED
     * 
     * @param orderId ID de la commande
     * @param courierId ID du livreur
     * @return OrderResponse avec status=DELIVERED et actualDeliveryTime
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    OrderResponse markAsDelivered(Long orderId, Long courierId);

    // ==================== RECHERCHE CLIENT ====================

    /**
     * Obtenir l'historique des commandes d'un client
     * 
     * @param customerId ID du client
     * @param pageable Pagination
     * @return Page<OrderResponse> commandes du client
     */
    Page<OrderResponse> getCustomerOrders(Long customerId, Pageable pageable);

    /**
     * Obtenir les commandes actives d'un client
     * 
     * @param customerId ID du client
     * @return List<OrderResponse> commandes en cours
     */
    List<OrderResponse> getActiveOrdersByCustomer(Long customerId);

    // ==================== RECHERCHE PARTENAIRE ====================

    /**
     * Obtenir les commandes d'un partenaire
     * 
     * @param partnerId ID du partenaire
     * @param pageable Pagination
     * @return Page<OrderResponse> commandes du partenaire
     */
    Page<OrderResponse> getPartnerOrders(Long partnerId, Pageable pageable);

    /**
     * Obtenir les commandes d'un partenaire par statut
     * 
     * @param partnerId ID du partenaire
     * @param status Statut recherché
     * @param pageable Pagination
     * @return Page<OrderResponse> commandes filtrées
     */
    Page<OrderResponse> getPartnerOrdersByStatus(Long partnerId, OrderStatus status, Pageable pageable);

    /**
     * Obtenir les commandes actives d'un partenaire
     * 
     * @param partnerId ID du partenaire
     * @return List<OrderResponse> commandes en cours
     */
    List<OrderResponse> getActiveOrdersByPartner(Long partnerId);

    // ==================== RECHERCHE LIVREUR ====================

    /**
     * Obtenir les commandes d'un livreur
     * 
     * @param courierId ID du livreur
     * @param pageable Pagination
     * @return Page<OrderResponse> commandes du livreur
     */
    Page<OrderResponse> getCourierOrders(Long courierId, Pageable pageable);

    /**
     * Obtenir les commandes actives d'un livreur
     * 
     * @param courierId ID du livreur
     * @return List<OrderResponse> commandes en cours
     */
    List<OrderResponse> getActiveOrdersByCourier(Long courierId);

    // ==================== RECHERCHE GÉNÉRALE ====================

    /**
     * Obtenir toutes les commandes avec pagination
     * 
     * @param pageable Pagination
     * @return Page<OrderResponse>
     */
    Page<OrderResponse> getAllOrders(Pageable pageable);

    /**
     * Obtenir les commandes par statut
     * 
     * @param status Statut recherché
     * @param pageable Pagination
     * @return Page<OrderResponse>
     */
    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    /**
     * Obtenir les commandes par période
     * 
     * @param start Date de début
     * @param end Date de fin
     * @param pageable Pagination
     * @return Page<OrderResponse>
     */
    Page<OrderResponse> getOrdersByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Obtenir les commandes en attente de livreur
     * 
     * @return List<OrderResponse> commandes à assigner
     */
    List<OrderResponse> getOrdersAwaitingCourier();

    // ==================== TRACKING ====================

    /**
     * Obtenir les informations de tracking d'une commande
     * 
     * @param orderId ID de la commande
     * @return OrderResponse avec historique complet des statuts
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    OrderResponse trackOrder(Long orderId);

    /**
     * Mettre à jour l'heure estimée de livraison
     * 
     * @param orderId ID de la commande
     * @param estimatedTime Nouvelle heure estimée
     * @return OrderResponse mis à jour
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    OrderResponse updateEstimatedDeliveryTime(Long orderId, LocalDateTime estimatedTime);

    // ==================== FILTRAGE PARTENAIRE (server-side) ====================

    /**
     * Commandes d'un partenaire avec filtrage serveur (statut + recherche textuelle).
     *
     * @param partnerId ID du partenaire
     * @param status    Filtre statut (null = tous)
     * @param search    Recherche sur numéro commande ou nom client (null/"" = aucun filtre)
     * @param pageable  Pagination et tri
     */
    Page<OrderResponse> getPartnerOrdersFiltered(
            Long partnerId,
            OrderStatus status,
            String search,
            Pageable pageable,
            String sortBy,
            LocalDateTime from,
            LocalDateTime to,
            String cancelledBy
    );

    /**
     * Toutes les commandes correspondant aux filtres partenaire (tri {@code orderTime} desc),
     * chargées par pages internes — pour export serveur.
     */
    List<OrderResponse> listAllPartnerOrdersForExport(
            Long partnerId,
            OrderStatus status,
            String search,
            LocalDateTime from,
            LocalDateTime to,
            String cancelledBy
    );

    /**
     * Résumé KPI pour l’historique partenaire (total, CA livrées, annulations, taux).
     */
    PartnerOrderHistorySummaryDTO getPartnerOrderHistorySummary(
            Long partnerId,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * Historique des statuts d'une commande avec filtres optionnels (côté serveur).
     *
     * @param status    filtre sur le statut de l'événement (null = tous)
     * @param actorType filtre sur le type d'acteur SYSTEM, PARTNER, etc. (null/vide = tous)
     * @param from      début inclus, ISO-8601 local {@code yyyy-MM-dd'T'HH:mm:ss} (null = sans borne)
     * @param to        fin inclusive, même format (null = sans borne)
     * @param pageable  pagination (page, size)
     */
    Page<OrderResponse.StatusHistoryDTO> getOrderHistory(
            Long orderId,
            OrderStatus status,
            String actorType,
            String from,
            String to,
            Pageable pageable
    );

    /**
     * Compteurs par statut pour les chips (alignés sur le filtre date de la liste).
     *
     * @param from début sur {@code orderTime} (null = sans borne basse)
     * @param to   fin inclusive (null = sans borne haute)
     */
    java.util.Map<String, Long> getPartnerOrderCounts(Long partnerId, LocalDateTime from, LocalDateTime to);

    /**
     * Document HTML (ticket cuisine 80&nbsp;mm) pour impression — réservé au partenaire propriétaire.
     */
    String buildKitchenTicketHtml(Long orderId, Long partnerId);

    // ==================== STATS INTERNES ====================

    java.util.Map<String, Long> getDailyStatsByPartners(List<Long> partnerIds, int days);

    // ==================== ADMIN ====================

    /**
     * Liste paginée de toutes les commandes pour le admin panel.
     */
    Page<OrderResponse> getAdminOrders(OrderStatus status, String paymentMethod, String paymentStatus,
                                       String search,
                                       LocalDateTime startDate, LocalDateTime endDate,
                                       Long partnerId, Long courierId,
                                       BigDecimal amountMin, BigDecimal amountMax,
                                       Boolean scheduledOnly,
                                       Pageable pageable);

    /**
     * Statistiques admin : KPIs + ventilation horaire ou journalière.
     */
    OrderStatsResponse getAdminStats(LocalDate date, String granularity);

    /**
     * Rembourser une commande (total ou partiel).
     */
    OrderResponse refundOrder(Long orderId, BigDecimal amount);

    /**
     * Toutes les commandes admin (sans pagination) pour export.
     */
    List<OrderResponse> listAllAdminOrdersForExport(
            OrderStatus status, String paymentMethod, String paymentStatus,
            String search, LocalDateTime startDate, LocalDateTime endDate,
            Long partnerId, Long courierId, BigDecimal amountMin, BigDecimal amountMax,
            Boolean scheduledOnly);

    /**
     * Logs admin paginés : historique de toutes les modifications.
     */
    Page<AdminLogDTO> getAdminLogs(String actorType, OrderStatus status, Long orderId,
                                   LocalDateTime from, LocalDateTime to, Pageable pageable);
}
