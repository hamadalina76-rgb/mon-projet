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
import com.speedline.order.dto.OrderItemDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Génération Excel / PDF pour l’export historique partenaire (mêmes filtres que la liste paginée).
 */
@Service
@RequiredArgsConstructor
public class PartnerOrderExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderService orderService;

    public byte[] exportExcel(
            Long partnerId,
            OrderStatus status,
            String search,
            LocalDateTime from,
            LocalDateTime to,
            String cancelledBy,
            String lang
    ) {
        final PartnerOrderExportLabels.Bundle labels = PartnerOrderExportLabels.bundle(lang);
        final List<OrderResponse> orders = orderService.listAllPartnerOrdersForExport(
                partnerId, status, search, from, to, cancelledBy);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            final Sheet sheet = wb.createSheet(labels.sheetName());
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
            c0.setCellValue(labels.documentTitle());
            c0.setCellStyle(titleStyle);

            Row periodRow = sheet.createRow(rowIdx++);
            periodRow.createCell(0).setCellValue(formatPeriod(from, to, labels));

            Row filterRow = sheet.createRow(rowIdx++);
            filterRow.createCell(0).setCellValue(buildFilterLine(status, cancelledBy, search, labels));

            rowIdx++;

            Row headRow = sheet.createRow(rowIdx++);
            String[] headers = columnHeaders(labels);
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
            Long partnerId,
            OrderStatus status,
            String search,
            LocalDateTime from,
            LocalDateTime to,
            String cancelledBy,
            String lang
    ) {
        final PartnerOrderExportLabels.Bundle labels = PartnerOrderExportLabels.pdfBundle(lang);
        final List<OrderResponse> orders = orderService.listAllPartnerOrdersForExport(
                partnerId, status, search, from, to, cancelledBy);
        try {
            return buildPdf(orders, status, cancelledBy, search, from, to, labels);
        } catch (DocumentException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Export PDF indisponible", e);
        }
    }

    private byte[] buildPdf(
            List<OrderResponse> orders,
            OrderStatus status,
            String cancelledBy,
            String search,
            LocalDateTime from,
            LocalDateTime to,
            PartnerOrderExportLabels.Bundle labels
    ) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 48, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();
        try {
            var titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            var normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            var smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            document.add(new Paragraph(labels.documentTitle(), titleFont));
            document.add(new Paragraph(formatPeriod(from, to, labels), smallFont));
            document.add(new Paragraph(buildFilterLine(status, cancelledBy, search, labels), smallFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.1f, 0.9f, 0.65f, 1.1f, 2.0f, 1.0f, 1.0f, 1.0f, 1.1f, 1.0f});

            for (String h : columnHeaders(labels)) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                cell.setPadding(4);
                table.addCell(cell);
            }

            for (OrderResponse o : orders) {
                for (String v : rowCells(o, labels)) {
                    PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : v, normalFont));
                    cell.setPadding(3);
                    table.addCell(cell);
                }
            }

            document.add(table);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return baos.toByteArray();
    }

    private static String[] columnHeaders(PartnerOrderExportLabels.Bundle b) {
        return new String[]{
                b.colOrderNumber(),
                b.colDate(),
                b.colTime(),
                b.colCustomer(),
                b.colItems(),
                b.colHt(),
                b.colTva(),
                b.colTtc(),
                b.colStatus(),
                b.colCourier()
        };
    }

    private static String formatPeriod(LocalDateTime from, LocalDateTime to, PartnerOrderExportLabels.Bundle b) {
        String fromS = from != null ? from.toLocalDate().toString() : "—";
        String toS = to != null ? to.toLocalDate().toString() : "—";
        return String.format(b.periodTemplate(), fromS, toS);
    }

    private static String buildFilterLine(
            OrderStatus status,
            String cancelledBy,
            String search,
            PartnerOrderExportLabels.Bundle b
    ) {
        List<String> parts = new ArrayList<>();
        parts.add(filterStatusFragment(status, cancelledBy, b));
        if (search != null && !search.isBlank()) {
            parts.add(b.filterSearchPrefix() + " " + search.trim());
        }
        return String.join(" · ", parts);
    }

    private static String filterStatusFragment(
            OrderStatus status,
            String cancelledBy,
            PartnerOrderExportLabels.Bundle b
    ) {
        if (status == null) {
            return b.filterLineAll();
        }
        if (status == OrderStatus.DELIVERED) {
            return b.filterLineDelivered();
        }
        if (status == OrderStatus.CANCELLED) {
            if (cancelledBy != null && "PARTNER".equalsIgnoreCase(cancelledBy.trim())) {
                return b.filterLineRefused();
            }
            return b.filterLineCancelled();
        }
        String label = b.statusLabels().getOrDefault(status, status.name());
        return b.filterStatusPrefix() + " " + label;
    }

    private static String[] rowCells(OrderResponse o, PartnerOrderExportLabels.Bundle labels) {
        LocalDateTime ca = o.getCreatedAt() != null ? o.getCreatedAt() : o.getOrderTime();
        String dateStr = ca != null ? ca.format(DATE_FMT) : "";
        String timeStr = ca != null ? ca.format(TIME_FMT) : "";
        String cust = o.getCustomerName() != null && !o.getCustomerName().isBlank()
                ? o.getCustomerName()
                : labels.dash();
        Amounts amt = exportAmounts(o);
        OrderStatus st = o.getStatus();
        String stLabel = st != null ? labels.statusLabels().getOrDefault(st, st.name()) : "";
        String courier = o.getCourierName() != null && !o.getCourierName().isBlank()
                ? o.getCourierName()
                : labels.dash();

        return new String[]{
                o.getOrderNumber() != null ? o.getOrderNumber() : "",
                dateStr,
                timeStr,
                cust,
                itemsSummary(o),
                formatTnd(amt.ht()),
                formatTnd(amt.tva()),
                formatTnd(amt.ttc()),
                stLabel,
                courier
        };
    }

    private static String itemsSummary(OrderResponse o) {
        List<OrderItemDTO> items = o.getItems() != null ? o.getItems() : List.of();
        if (items.isEmpty()) {
            return "";
        }
        String first = items.get(0).getProductName() != null ? items.get(0).getProductName() : "";
        return items.size() > 1 ? first + " +" + (items.size() - 1) : first;
    }

    private record Amounts(BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
    }

    private static Amounts exportAmounts(OrderResponse o) {
        BigDecimal ttc = nullToZero(o.getTotal());
        BigDecimal tax = o.getTax();
        BigDecimal tva;
        BigDecimal ht;
        if (tax != null) {
            tva = tax;
            ht = ttc.subtract(tax);
        } else {
            tva = BigDecimal.ZERO;
            ht = nullToZero(o.getSubtotal());
        }
        return new Amounts(ht, tva, ttc);
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
}
