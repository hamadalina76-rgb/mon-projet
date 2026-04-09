package com.speedline.order.service.kitchen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.order.domain.Order;
import com.speedline.order.domain.OrderItem;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Génère un document HTML autonome (styles intégrés) — ticket cuisine thermique 80 mm style Glovo/Uber Eats.
 * Centré sur la page, police monospace, compatible imprimante thermique.
 */
public final class KitchenTicketHtmlBuilder {

    private static final DateTimeFormatter DT_FMT    = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm");

    private KitchenTicketHtmlBuilder() {
    }

    public static String buildDocument(Order order, List<OrderItem> items, ObjectMapper objectMapper) {

        /* ── Temps ──────────────────────────────────────────────────────── */
        LocalDateTime when   = order.getOrderTime() != null ? order.getOrderTime() : order.getCreatedAt();
        String dateStr = when != null ? when.format(DT_FMT)   : "—";
        String timeStr = when != null ? when.format(TIME_FMT) : "—";

        /* ── Type / mode ────────────────────────────────────────────────── */
        Order.OrderType type = order.getType() != null ? order.getType() : Order.OrderType.DELIVERY;
        String typeLabel = type == Order.OrderType.PICKUP ? "À EMPORTER" : "LIVRAISON À DOMICILE";
        String typeIcon  = type == Order.OrderType.PICKUP ? "🛍️" : "🛵";

        /* ── Partenaire ─────────────────────────────────────────────────── */
        String partnerName = esc(nvl(order.getPartnerName(), "Cuisine"));
        String partnerAddr = esc(nvl(order.getPartnerAddress(), ""));
        String partnerTel  = esc(nvl(order.getPartnerPhone(), ""));

        /* ── Client ─────────────────────────────────────────────────────── */
        String clientName  = esc(nvl(order.getCustomerName(), "—"));
        String clientTel   = esc(nvl(order.getCustomerPhone(), ""));

        /* ── Adresse livraison ──────────────────────────────────────────── */
        String deliveryAddr = buildDeliveryAddr(order);

        /* ── Paiement ───────────────────────────────────────────────────── */
        String payMethod    = formatPayment(order);

        /* ── Totaux ─────────────────────────────────────────────────────── */
        String subtotalStr  = fmt(order.getSubtotal());
        String deliveryStr  = fmt(order.getDeliveryFee());
        BigDecimal total    = order.getTotal() != null ? order.getTotal()
                            : safeAdd(order.getSubtotal(), order.getDeliveryFee());
        String totalStr     = fmt(total);

        /* ── Numéro de commande ─────────────────────────────────────────── */
        String orderNum     = esc(order.getOrderNumber());

        /* ── Prép. estimée ──────────────────────────────────────────────── */
        String prepInfo = "";
        if (order.getSuggestedPreparationMinutes() != null && order.getSuggestedPreparationMinutes() > 0) {
            prepInfo = "<div class=\"prep-row\">⏱ Préparation estimée : <b>"
                    + order.getSuggestedPreparationMinutes() + " min</b></div>";
        }

        /* ── Articles ───────────────────────────────────────────────────── */
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem it : items) {
            itemsHtml.append(buildItemBlock(it, objectMapper));
        }

        /* ── Notes client ───────────────────────────────────────────────── */
        String notesBlock = "";
        String cn = order.getCustomerNotes();
        if (cn != null && !cn.isBlank()) {
            notesBlock = "<div class=\"section-label\">📝 Note du client</div>"
                       + "<div class=\"note-box\">" + esc(cn) + "</div>";
        }

        /* ── Instructions livraison ─────────────────────────────────────── */
        String diBlock = "";
        String di = order.getDeliveryInstructions();
        if (di != null && !di.isBlank()) {
            diBlock = "<div class=\"section-label\">📍 Instructions livraison</div>"
                    + "<div class=\"note-box\">" + esc(di) + "</div>";
        }

        /* ── Adresse livraison (section) ────────────────────────────────── */
        String addrSection = "";
        if (type == Order.OrderType.DELIVERY && !deliveryAddr.isBlank()) {
            addrSection = "<div class=\"section-label\">📍 Adresse de livraison</div>"
                        + "<div class=\"addr-box\">" + deliveryAddr + "</div>";
        }

        /* ── Assemblage ─────────────────────────────────────────────────── */
        return "<!DOCTYPE html>"
            + "<html lang=\"fr\" style=\"width:80mm;margin:0 auto;\">"
            + "<head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=80,initial-scale=1\">"
            + "<title>Ticket #" + orderNum + "</title>"
            + "<style>" + CSS + "</style>"
            + "</head><body>"
            + "<div class=\"ticket\">"

            /* ══ EN-TÊTE ═══════════════════════════════════════════════════ */
            + "<div class=\"header\">"
            +   "<div class=\"brand\">SPEEDLINE</div>"
            +   "<div class=\"partner-name\">" + partnerName + "</div>"
            +   (partnerAddr.isEmpty() ? "" : "<div class=\"partner-detail\">" + partnerAddr + "</div>")
            +   (partnerTel.isEmpty()  ? "" : "<div class=\"partner-detail\">☎ " + partnerTel + "</div>")
            + "</div>"

            /* ══ NUMÉRO + HORODATAGE ════════════════════════════════════════ */
            + "<div class=\"rule-thick\"></div>"
            + "<div class=\"order-num\">Commande #" + orderNum + "</div>"
            + "<div class=\"meta-row\"><span class=\"meta-label\">Date</span><span class=\"meta-val\">" + dateStr + "</span></div>"
            + "<div class=\"meta-row\"><span class=\"meta-label\">Heure</span><span class=\"meta-val\">" + timeStr + "</span></div>"

            /* ══ TYPE DE COMMANDE ═══════════════════════════════════════════ */
            + "<div class=\"type-badge\">" + typeIcon + " " + typeLabel + "</div>"

            + prepInfo

            /* ══ CLIENT ════════════════════════════════════════════════════ */
            + "<div class=\"rule-dash\"></div>"
            + "<div class=\"section-label\">👤 Client</div>"
            + "<div class=\"meta-row\"><span class=\"meta-label\">Nom</span><span class=\"meta-val\">" + clientName + "</span></div>"
            + (clientTel.isEmpty() ? "" : "<div class=\"meta-row\"><span class=\"meta-label\">Tél</span><span class=\"meta-val\">" + clientTel + "</span></div>")
            + addrSection

            /* ══ ARTICLES ══════════════════════════════════════════════════ */
            + "<div class=\"rule-dash\"></div>"
            + "<div class=\"section-label\">🍽️ Articles</div>"
            + "<div class=\"items\">" + itemsHtml + "</div>"

            /* ══ TOTAUX ════════════════════════════════════════════════════ */
            + "<div class=\"rule-thin\"></div>"
            + "<div class=\"meta-row\"><span class=\"meta-label\">Sous-total</span><span class=\"meta-val\">" + subtotalStr + " TND</span></div>"
            + (type == Order.OrderType.DELIVERY ? "<div class=\"meta-row\"><span class=\"meta-label\">Livraison</span><span class=\"meta-val\">" + deliveryStr + " TND</span></div>" : "")
            + "<div class=\"total-row\"><span>TOTAL</span><span>" + totalStr + " TND</span></div>"
            + "<div class=\"pay-row\">" + payMethod + "</div>"

            /* ══ NOTES ═════════════════════════════════════════════════════ */
            + (notesBlock.isEmpty() && diBlock.isEmpty() ? "" : "<div class=\"rule-dash\"></div>")
            + notesBlock
            + diBlock

            /* ══ PIED DE PAGE ══════════════════════════════════════════════ */
            + "<div class=\"rule-thick\"></div>"
            + "<div class=\"footer\">Merci pour votre commande !</div>"
            + "<div class=\"footer footer-sm\">speedline.tn</div>"

            + "</div>"
            + "</body></html>";
    }

    /* ──────────────────────────────────────────────────────────────────────── */

    private static String buildItemBlock(OrderItem it, ObjectMapper objectMapper) {
        String name   = esc(it.getProductName());
        int    qty    = it.getQuantity() != null ? it.getQuantity() : 1;
        String price  = it.getSubtotal() != null ? " <span class=\"item-price\">" + fmt(it.getSubtotal()) + " TND</span>" : "";

        StringBuilder inner = new StringBuilder();
        inner.append("<div class=\"item-head\">")
             .append("<span class=\"item-qty\">").append(qty).append("×</span>")
             .append("<span class=\"item-name\">").append(name).append("</span>")
             .append(price)
             .append("</div>");

        for (String line : parseOptionsJson(it.getSelectedOptionsJson(), objectMapper)) {
            inner.append("<div class=\"mod\">").append(esc(line)).append("</div>");
        }
        for (String line : parseAddonsJson(it.getSelectedAddonsJson(), objectMapper)) {
            inner.append("<div class=\"mod\">").append(esc(line)).append("</div>");
        }
        if (it.getSpecialInstructions() != null && !it.getSpecialInstructions().isBlank()) {
            inner.append("<div class=\"item-note\">📌 ").append(esc(it.getSpecialInstructions())).append("</div>");
        }
        return "<div class=\"item\">" + inner + "</div>";
    }

    private static List<String> parseOptionsJson(String json, ObjectMapper mapper) {
        List<String> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        try {
            JsonNode root = mapper.readTree(json);
            if (!root.isArray()) return out;
            for (JsonNode n : root) {
                String on = text(n, "optionName");
                String vn = text(n, "valueName");
                if (!vn.isBlank()) out.add("  ↳ " + on + " : " + vn);
                else if (!on.isBlank()) out.add("  ↳ " + on);
            }
        } catch (Exception ignored) { /* resilient */ }
        return out;
    }

    private static List<String> parseAddonsJson(String json, ObjectMapper mapper) {
        List<String> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        try {
            JsonNode root = mapper.readTree(json);
            if (!root.isArray()) return out;
            for (JsonNode n : root) {
                String an = text(n, "addonName");
                if (an.isBlank()) continue;
                int q = n.path("quantity").asInt(1);
                out.add(q > 1 ? "  ↳ " + an + " ×" + q : "  ↳ " + an);
            }
        } catch (Exception ignored) { /* resilient */ }
        return out;
    }

    private static String buildDeliveryAddr(Order order) {
        StringBuilder sb = new StringBuilder();
        String raw = order.getDeliveryInstructions(); /* fallback */
        /* L'adresse sérialisée est dans deliveryAddressJson — on la laisse côté frontend
           pour l'affichage détaillé ; ici on utilise les champs plats si présents */
        if (sb.isEmpty() && raw != null && !raw.isBlank()) sb.append(esc(raw));
        return sb.toString();
    }

    private static String formatPayment(Order order) {
        if (order.getPaymentMethod() == null) return "";
        return switch (order.getPaymentMethod().name()) {
            case "CASH"             -> "💵 Paiement en espèces";
            case "CARD"             -> "💳 Carte bancaire";
            case "WALLET"           -> "📱 Portefeuille électronique";
            case "CARD_ON_DELIVERY" -> "💳 Carte à la livraison";
            default                 -> esc(order.getPaymentMethod().name());
        };
    }

    private static String fmt(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format("%.2f", v);
    }

    private static BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        return (a != null ? a : BigDecimal.ZERO).add(b != null ? b : BigDecimal.ZERO);
    }

    private static String nvl(String v, String fallback) {
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    private static String text(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) return "";
        return n.get(field).asText("");
    }

    private static String esc(String raw) {
        if (raw == null) return "";
        return HtmlUtils.htmlEscape(raw);
    }

    /* ─────────────────────────────────── CSS ───────────────────────────────── */
    private static final String CSS = """
            @page {
              size: 80mm auto;
              margin: 0;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            html {
              width: 80mm;
              background: #fff;
            }
            body {
              width: 80mm;
              margin: 0 auto;
              padding: 4mm 3mm 6mm;
              background: #fff;
              color: #000;
              font-family: 'Courier New', Courier, monospace;
              font-size: 11px;
              line-height: 1.4;
              -webkit-print-color-adjust: exact;
              print-color-adjust: exact;
            }
            .ticket { width: 100%; }

            /* ── En-tête ─────────────────────────────────── */
            .header {
              text-align: center;
              padding-bottom: 5px;
              margin-bottom: 5px;
              border-bottom: 2px solid #000;
            }
            .brand {
              font-size: 17px;
              font-weight: 900;
              letter-spacing: 0.22em;
              border: 2px solid #000;
              display: inline-block;
              padding: 2px 10px;
              margin-bottom: 4px;
            }
            .partner-name {
              font-size: 12px;
              font-weight: 800;
              text-transform: uppercase;
              letter-spacing: 0.04em;
              margin-bottom: 2px;
            }
            .partner-detail { font-size: 9px; color: #333; }

            /* ── Numéro de commande ───────────────────────── */
            .order-num {
              text-align: center;
              font-size: 14px;
              font-weight: 900;
              padding: 5px 0 3px;
              border-bottom: 1px dashed #000;
              margin-bottom: 3px;
            }

            /* ── Ligne méta (label : valeur) ─────────────── */
            .meta-row {
              display: flex;
              justify-content: space-between;
              font-size: 10px;
              padding: 1px 0;
            }
            .meta-label { color: #444; }
            .meta-val   { font-weight: 700; }

            /* ── Badge type ───────────────────────────────── */
            .type-badge {
              text-align: center;
              font-size: 10.5px;
              font-weight: 900;
              letter-spacing: 0.06em;
              border: 2px solid #000;
              padding: 4px;
              margin: 5px 0 3px;
              background: #000;
              color: #fff;
            }

            /* ── Prépa estimée ────────────────────────────── */
            .prep-row {
              text-align: center;
              font-size: 9.5px;
              padding: 2px 0 3px;
            }

            /* ── Séparateurs ─────────────────────────────── */
            .rule-thick {
              border: none;
              border-top: 2px double #000;
              margin: 5px 0;
            }
            .rule-dash {
              border: none;
              border-top: 1px dashed #000;
              margin: 5px 0;
            }
            .rule-thin {
              border: none;
              border-top: 1px solid #000;
              margin: 4px 0;
            }

            /* ── Titres de section ────────────────────────── */
            .section-label {
              font-size: 9px;
              font-weight: 900;
              letter-spacing: 0.1em;
              text-transform: uppercase;
              margin-bottom: 3px;
            }

            /* ── Articles ────────────────────────────────── */
            .items { margin: 0; }
            .item {
              padding: 4px 0;
              border-bottom: 1px dotted #888;
            }
            .item:last-child { border-bottom: none; }
            .item-head {
              display: flex;
              align-items: baseline;
              gap: 4px;
              font-weight: 800;
              font-size: 11px;
            }
            .item-qty { flex-shrink: 0; min-width: 1.8em; }
            .item-name { flex: 1; word-break: break-word; }
            .item-price {
              flex-shrink: 0;
              font-size: 9.5px;
              white-space: nowrap;
            }
            .mod {
              font-size: 9px;
              font-weight: 600;
              color: #333;
              padding-left: 12px;
              line-height: 1.5;
            }
            .item-note {
              margin-top: 3px;
              padding: 3px 5px;
              font-size: 9px;
              font-style: italic;
              border-left: 2px solid #000;
              background: #f0f0f0;
            }

            /* ── Totaux ──────────────────────────────────── */
            .total-row {
              display: flex;
              justify-content: space-between;
              font-size: 12px;
              font-weight: 900;
              border-top: 2px solid #000;
              border-bottom: 2px solid #000;
              padding: 4px 0;
              margin: 3px 0;
            }
            .pay-row {
              text-align: center;
              font-size: 9.5px;
              font-weight: 700;
              padding: 2px 0;
            }

            /* ── Notes / adresse ─────────────────────────── */
            .note-box {
              font-size: 9.5px;
              border: 1px dashed #000;
              padding: 4px 5px;
              margin-bottom: 3px;
              white-space: pre-wrap;
              word-break: break-word;
              line-height: 1.4;
            }
            .addr-box {
              font-size: 9.5px;
              padding: 2px 0 3px;
              word-break: break-word;
            }

            /* ── Pied de page ────────────────────────────── */
            .footer {
              text-align: center;
              font-size: 10px;
              font-weight: 700;
              padding-top: 4px;
            }
            .footer-sm {
              font-size: 8.5px;
              font-weight: 400;
              color: #444;
              padding-top: 1px;
            }

            @media print {
              html, body { width: 80mm; margin: 0; padding: 3mm 2mm 5mm; }
              .ticket { width: 100%; }
            }
            """;
}
