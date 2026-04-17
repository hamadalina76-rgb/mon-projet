package com.speedline.order.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.speedline.order.domain.OrderStatus;
import com.speedline.order.dto.OrderResponse;
import com.speedline.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminOrderExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderService orderService;

    public byte[] exportExcel(
            OrderStatus status, String paymentMethod, String paymentStatus,
            String search, LocalDateTime startDate, LocalDateTime endDate,
            Long partnerId, Long courierId, BigDecimal amountMin, BigDecimal amountMax,
            Boolean scheduledOnly,
            String lang
    ) {
        final Labels labels = Labels.of(lang);
        final List<OrderResponse> orders = orderService.listAllAdminOrdersForExport(
                status, paymentMethod, paymentStatus, search, startDate, endDate,
                partnerId, courierId, amountMin, amountMax, scheduledOnly);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(labels.sheetName);
            int rowIdx = 0;

            org.apache.poi.ss.usermodel.Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);

            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);

            Row titleRow = sheet.createRow(rowIdx++);
            Cell c0 = titleRow.createCell(0);
            c0.setCellValue(labels.docTitle);
            c0.setCellStyle(titleStyle);

            Row periodRow = sheet.createRow(rowIdx++);
            periodRow.createCell(0).setCellValue(formatPeriod(startDate, endDate, labels));
            rowIdx++;

            Row headRow = sheet.createRow(rowIdx++);
            String[] headers = labels.columns();
            for (int i = 0; i < headers.length; i++) {
                Cell hc = headRow.createCell(i);
                hc.setCellValue(headers[i]);
                hc.setCellStyle(headerStyle);
            }

            for (OrderResponse o : orders) {
                Row dataRow = sheet.createRow(rowIdx++);
                String[] cells = rowCells(o, labels);
                for (int i = 0; i < cells.length; i++) {
                    dataRow.createCell(i).setCellValue(cells[i]);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Export Excel indisponible", e);
        }
    }

    public byte[] exportPdf(
            OrderStatus status, String paymentMethod, String paymentStatus,
            String search, LocalDateTime startDate, LocalDateTime endDate,
            Long partnerId, Long courierId, BigDecimal amountMin, BigDecimal amountMax,
            Boolean scheduledOnly,
            String lang
    ) {
        final Labels labels = Labels.of(lang);
        final List<OrderResponse> orders = orderService.listAllAdminOrdersForExport(
                status, paymentMethod, paymentStatus, search, startDate, endDate,
                partnerId, courierId, amountMin, amountMax, scheduledOnly);
        try {
            return buildPdf(orders, startDate, endDate, labels);
        } catch (DocumentException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Export PDF indisponible", e);
        }
    }

    private byte[] buildPdf(List<OrderResponse> orders, LocalDateTime from, LocalDateTime to, Labels labels)
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 48, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();
        try {
            var titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            var normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            var smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            document.add(new Paragraph(labels.docTitle, titleFont));
            document.add(new Paragraph(formatPeriod(from, to, labels), smallFont));
            document.add(new Paragraph(" "));

            String[] headers = labels.columns();
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.1f, 0.8f, 0.5f, 1.0f, 1.0f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f});

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7)));
                cell.setPadding(3);
                table.addCell(cell);
            }

            for (OrderResponse o : orders) {
                for (String v : rowCells(o, labels)) {
                    PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : v, normalFont));
                    cell.setPadding(2);
                    table.addCell(cell);
                }
            }

            document.add(table);
        } finally {
            if (document.isOpen()) document.close();
        }
        return baos.toByteArray();
    }

    private static String formatPeriod(LocalDateTime from, LocalDateTime to, Labels labels) {
        String fromS = from != null ? from.toLocalDate().toString() : "—";
        String toS = to != null ? to.toLocalDate().toString() : "—";
        return labels.periodPrefix + " " + fromS + " → " + toS;
    }

    private static String[] rowCells(OrderResponse o, Labels labels) {
        LocalDateTime ca = o.getCreatedAt() != null ? o.getCreatedAt() : o.getOrderTime();
        String dateStr = ca != null ? ca.format(DATE_FMT) : "";
        String timeStr = ca != null ? ca.format(TIME_FMT) : "";
        String cust = o.getCustomerName() != null ? o.getCustomerName() : "—";
        String partner = o.getPartnerName() != null ? o.getPartnerName() : "—";
        String courier = o.getCourierName() != null ? o.getCourierName() : "—";
        String statusLabel = o.getStatus() != null ? o.getStatus().name() : "";
        String paymentMethod = o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "—";
        String paymentStatus = o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "—";

        return new String[]{
                o.getOrderNumber() != null ? o.getOrderNumber() : "",
                dateStr,
                timeStr,
                cust,
                partner,
                courier,
                formatTnd(nullToZero(o.getTotal())),
                paymentMethod,
                paymentStatus,
                statusLabel,
                o.getCancellationReason() != null ? o.getCancellationReason() : ""
        };
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String formatTnd(BigDecimal n) {
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.FRANCE);
        DecimalFormat df = new DecimalFormat("#,##0.000", sym);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(n.setScale(3, RoundingMode.HALF_UP)) + " TND";
    }

    private record Labels(
            String sheetName, String docTitle, String periodPrefix,
            String colOrder, String colDate, String colTime, String colCustomer,
            String colPartner, String colCourier, String colTotal,
            String colPayMethod, String colPayStatus, String colStatus, String colReason
    ) {
        String[] columns() {
            return new String[]{colOrder, colDate, colTime, colCustomer, colPartner, colCourier,
                    colTotal, colPayMethod, colPayStatus, colStatus, colReason};
        }

        static Labels of(String rawLang) {
            String lang = rawLang == null ? "fr" : rawLang.trim().toLowerCase(Locale.ROOT);
            if (lang.startsWith("en")) return en();
            if (lang.startsWith("ar")) return en(); // PDF Helvetica has no Arabic glyphs
            return fr();
        }

        static Labels fr() {
            return new Labels(
                    "Commandes", "SpeedLine — Export commandes (Admin)", "Période :",
                    "N° commande", "Date", "Heure", "Client",
                    "Partenaire", "Livreur", "Total TTC",
                    "Paiement", "Statut paiement", "Statut", "Motif annulation"
            );
        }

        static Labels en() {
            return new Labels(
                    "Orders", "SpeedLine — Orders export (Admin)", "Period:",
                    "Order #", "Date", "Time", "Customer",
                    "Partner", "Courier", "Total",
                    "Payment", "Payment status", "Status", "Cancel reason"
            );
        }
    }
}
