package com.speedline.partner.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsDTO {

    private Long   categoryId;
    private String categoryName;

    private Integer productCount;
    private Integer partnerCount;

    /** Nombre de commandes sur les 30 derniers jours (sera connecté à l'order-service) */
    private Integer ordersLast30Days;

    /** Tendance en % vs le mois précédent (positif = hausse) */
    private Double orderTrendPercent;
    private Double partnerTrendPercent;
    private Double productTrendPercent;

    /** true si ordersLast30Days dépasse le seuil configurable */
    private boolean topCategory;

    /** Seuil au-delà duquel une catégorie est "Top" (configurable) */
    private int topCategoryThreshold;

    /** 30 points pour le graphique (J-30 → J) */
    private List<DailyOrderStat> dailyOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyOrderStat {
        private String day;    // "01 Mar", …
        private int    orders;
    }
}
