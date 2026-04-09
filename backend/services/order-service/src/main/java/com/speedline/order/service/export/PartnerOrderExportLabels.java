package com.speedline.order.service.export;

import com.speedline.order.domain.OrderStatus;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Libellés export historique partenaire (alignés sur i18n partner-dashboard fr / en / ar).
 */
public final class PartnerOrderExportLabels {

    private PartnerOrderExportLabels() {
    }

    public record Bundle(
            String sheetName,
            String documentTitle,
            String periodTemplate,
            String filterStatusPrefix,
            String filterSearchPrefix,
            String filterLineAll,
            String filterLineDelivered,
            String filterLineCancelled,
            String filterLineRefused,
            String colOrderNumber,
            String colDate,
            String colTime,
            String colCustomer,
            String colItems,
            String colHt,
            String colTva,
            String colTtc,
            String colStatus,
            String colCourier,
            String dash,
            Map<OrderStatus, String> statusLabels
    ) {
    }

    public static Bundle bundle(String rawLang) {
        String lang = rawLang == null ? "fr" : rawLang.trim().toLowerCase(Locale.ROOT);
        if (lang.startsWith("ar")) {
            return ar();
        }
        if (lang.startsWith("en")) {
            return en();
        }
        return fr();
    }

    /**
     * PDF (Helvetica) : pas de glyphes arabes pour les en-têtes — on retombe sur l’anglais.
     */
    public static Bundle pdfBundle(String rawLang) {
        String lang = rawLang == null ? "fr" : rawLang.trim().toLowerCase(Locale.ROOT);
        if (lang.startsWith("ar")) {
            return en();
        }
        if (lang.startsWith("en")) {
            return en();
        }
        return fr();
    }

    private static Map<OrderStatus, String> statusMap(String p, String c, String pr, String r, String pi, String id, String d, String ca) {
        Map<OrderStatus, String> m = new EnumMap<>(OrderStatus.class);
        m.put(OrderStatus.PENDING, p);
        m.put(OrderStatus.CONFIRMED, c);
        m.put(OrderStatus.PREPARING, pr);
        m.put(OrderStatus.READY_FOR_PICKUP, r);
        m.put(OrderStatus.PICKED_UP, pi);
        m.put(OrderStatus.IN_DELIVERY, id);
        m.put(OrderStatus.DELIVERED, d);
        m.put(OrderStatus.CANCELLED, ca);
        return Map.copyOf(m);
    }

    private static Bundle fr() {
        return new Bundle(
                "Commandes",
                "SpeedLine — Export commandes",
                "Période : %s → %s",
                "Statut :",
                "Recherche :",
                "Statut : Tous",
                "Statut : Livrées",
                "Statut : Annulées",
                "Statut : Refusées",
                "N° commande",
                "Date",
                "Heure",
                "Client",
                "Articles",
                "Montant HT",
                "TVA",
                "Montant TTC",
                "Statut",
                "Livreur",
                "—",
                statusMap(
                        "En attente",
                        "Confirmée",
                        "En préparation",
                        "Prête",
                        "Récupérée",
                        "En livraison",
                        "Livrée",
                        "Annulée"
                )
        );
    }

    private static Bundle en() {
        return new Bundle(
                "Orders",
                "SpeedLine — Orders export",
                "Period: %s → %s",
                "Status:",
                "Search:",
                "Status: All",
                "Status: Delivered",
                "Status: Cancelled",
                "Status: Declined",
                "Order #",
                "Date",
                "Time",
                "Customer",
                "Items",
                "Amount excl. tax",
                "VAT",
                "Amount incl. tax",
                "Status",
                "Courier",
                "—",
                statusMap(
                        "Pending",
                        "Confirmed",
                        "Preparing",
                        "Ready",
                        "Picked up",
                        "Out for delivery",
                        "Delivered",
                        "Cancelled"
                )
        );
    }

    private static Bundle ar() {
        return new Bundle(
                "الطلبات",
                "SpeedLine — تصدير الطلبات",
                "الفترة: %s ← %s",
                "الحالة:",
                "بحث:",
                "الحالة: الكل",
                "الحالة: تم التسليم",
                "الحالة: ملغاة",
                "الحالة: مرفوضة",
                "رقم الطلب",
                "التاريخ",
                "الوقت",
                "العميل",
                "الأصناف",
                "المبلغ بدون ضريبة",
                "الضريبة",
                "المبلغ الإجمالي",
                "الحالة",
                "الساعي",
                "—",
                statusMap(
                        "قيد الانتظار",
                        "مؤكدة",
                        "قيد التحضير",
                        "جاهزة",
                        "تم الاستلام",
                        "قيد التوصيل",
                        "تم التسليم",
                        "ملغاة"
                )
        );
    }
}
