package com.speedline.analytics.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service pour les métriques et analytiques
 */
public interface AnalyticsService {

    /**
     * Obtenir les métriques du dashboard
     * 
     * @return DashboardMetricsDTO avec:
     *         - totalOrders: Nombre total de commandes
     *         - totalRevenue: Revenus totaux
     *         - activePartners: Nombre de partenaires actifs
     *         - activeCouriers: Nombre de livreurs actifs
     *         - averageDeliveryTime: Temps de livraison moyen
     *         - todayOrders: Commandes aujourd'hui
     *         - todayRevenue: Revenus aujourd'hui
     */
    DashboardMetricsDTO getDashboardMetrics();

    /**
     * Obtenir les statistiques de commandes
     * 
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return OrderStatsDTO avec:
     *         - totalOrders: Nombre total
     *         - completedOrders: Commandes complétées
     *         - cancelledOrders: Commandes annulées
     *         - averageOrderValue: Valeur moyenne
     *         - ordersByStatus: Distribution par statut
     */
    OrderStatsDTO getOrderStats(LocalDate startDate, LocalDate endDate);

    /**
     * Obtenir les revenus
     * 
     * @param startDate Date de début
     * @param endDate Date de fin
     * @param groupBy Grouper par (DAY, WEEK, MONTH)
     * @return List<RevenueDataDTO> revenus par période
     */
    List<RevenueDataDTO> getRevenue(LocalDate startDate, LocalDate endDate, String groupBy);

    /**
     * Obtenir les revenus d'un partenaire
     * 
     * @param partnerId ID du partenaire
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return RevenueDataDTO revenus du partenaire
     */
    RevenueDataDTO getPartnerRevenue(Long partnerId, LocalDate startDate, LocalDate endDate);

    /**
     * Obtenir la performance des partenaires
     * 
     * @param pageable Pagination
     * @return Page<PartnerPerformanceDTO> performance triée par revenus
     */
    Page<PartnerPerformanceDTO> getPartnerPerformance(Pageable pageable);

    /**
     * Obtenir la performance des livreurs
     * 
     * @param pageable Pagination
     * @return Page<CourierPerformanceDTO> performance triée par livraisons
     */
    Page<CourierPerformanceDTO> getCourierPerformance(Pageable pageable);

    /**
     * Enregistrer un événement analytique
     * Appelé automatiquement via Kafka consumers
     * 
     * @param analytics Données analytiques à enregistrer
     */
    void recordAnalytics(com.speedline.analytics.domain.Analytics analytics);

    /**
     * DTO pour les métriques du dashboard
     */
    record DashboardMetricsDTO(
            Long totalOrders,
            BigDecimal totalRevenue,
            Integer activePartners,
            Integer activeCouriers,
            Integer averageDeliveryTime,
            Long todayOrders,
            BigDecimal todayRevenue,
            Long pendingOrders,
            Long inDeliveryOrders
    ) {}

    /**
     * DTO pour les statistiques de commandes
     */
    record OrderStatsDTO(
            Long totalOrders,
            Long completedOrders,
            Long cancelledOrders,
            BigDecimal averageOrderValue,
            java.util.Map<String, Long> ordersByStatus,
            List<OrderTrendDTO> trends
    ) {}

    /**
     * DTO pour les données de revenus
     */
    record RevenueDataDTO(
            LocalDate date,
            BigDecimal totalRevenue,
            BigDecimal commission,
            Long orderCount,
            BigDecimal averageOrderValue
    ) {}

    /**
     * DTO pour la performance des partenaires
     */
    record PartnerPerformanceDTO(
            Long partnerId,
            String partnerName,
            Long totalOrders,
            BigDecimal totalRevenue,
            BigDecimal averageOrderValue,
            Integer averageRating,
            Double completionRate
    ) {}

    /**
     * DTO pour la performance des livreurs
     */
    record CourierPerformanceDTO(
            Long courierId,
            String courierName,
            Long totalDeliveries,
            Integer averageDeliveryTime,
            BigDecimal averageRating,
            Double successRate
    ) {}

    /**
     * DTO pour les tendances de commandes
     */
    record OrderTrendDTO(
            LocalDate date,
            Long orderCount,
            BigDecimal revenue
    ) {}
}
