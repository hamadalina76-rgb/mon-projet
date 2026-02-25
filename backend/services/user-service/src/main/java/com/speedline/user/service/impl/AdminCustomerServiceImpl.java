package com.speedline.user.service.impl;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.client.NotificationServiceClient;
import com.speedline.user.domain.Customer;
import com.speedline.user.domain.Customer.CustomerStatus;
import com.speedline.user.dto.ActivityLogRequest;
import com.speedline.user.dto.CustomerDTO;
import com.speedline.user.exception.CustomerNotFoundException;
import com.speedline.user.repository.AddressRepository;
import com.speedline.user.repository.CustomerRepository;
import com.speedline.user.service.ActivityLogService;
import com.speedline.user.service.AdminCustomerService;
import com.speedline.user.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private static final int EXPORT_PAGE_SIZE = 500;
    private static final int EXPORT_MAX_PAGES = 10;
    /** Borne min pour filtre date (évite LocalDateTime.MIN hors plage PostgreSQL). */
    private static final LocalDateTime DATE_FILTER_MIN = LocalDateTime.of(1970, 1, 1, 0, 0);
    /** Borne max pour filtre date (évite LocalDateTime.MAX hors plage PostgreSQL). */
    private static final LocalDateTime DATE_FILTER_MAX = LocalDateTime.of(2100, 12, 31, 23, 59, 59);

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final CustomerService customerService;
    private final AuthServiceClient authServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDTO> searchCustomers(CustomerStatus status, String search,
                                             LocalDateTime dateFrom, LocalDateTime dateTo,
                                             Pageable pageable) {
        LocalDateTime from = dateFrom != null ? dateFrom : DATE_FILTER_MIN;
        LocalDateTime to = dateTo != null ? dateTo : DATE_FILTER_MAX;
        List<Long> customerIdsFilter = resolveSearchFilter(search);
        if (customerIdsFilter != null && customerIdsFilter.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<Customer> page = customerRepository.findWithFilters(status, customerIdsFilter, from, to, pageable);
        return page.map(c -> customerService.getCustomerById(c.getId()));
    }

    /**
     * Résout le filtre de recherche globale (nom, email, téléphone, ville).
     * Retourne null si pas de recherche, liste vide si aucun résultat, liste non vide si des IDs à filtrer.
     */
    private List<Long> resolveSearchFilter(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String q = search.trim();
        Set<Long> ids = new HashSet<>();
        try {
            List<Long> userIds = authServiceClient.searchUserIds(q, "CUSTOMER");
            if (!userIds.isEmpty()) {
                customerRepository.findByUserIdIn(userIds).stream().map(Customer::getId).forEach(ids::add);
            }
            addressRepository.findCustomerIdsByCityContaining(q).forEach(ids::add);
        } catch (Exception e) {
            log.warn("Search filter resolution failed for q='{}': {}", q, e.getMessage());
        }
        return new ArrayList<>(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        return customerService.getCustomerById(id);
    }

    @Override
    public void blockCustomer(Long customerId, Long adminId, String adminName) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> CustomerNotFoundException.byId(customerId));
        authServiceClient.changeUserStatus(customer.getUserId(), "SUSPENDED");
        customerRepository.updateStatus(customerId, CustomerStatus.SUSPENDED);
        logAction(adminId, adminName, "BLOCK", customerId, customer.getUserId(), "Compte bloqué");
    }

    @Override
    public void unblockCustomer(Long customerId, Long adminId, String adminName) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> CustomerNotFoundException.byId(customerId));
        authServiceClient.changeUserStatus(customer.getUserId(), "ACTIVE");
        customerRepository.updateStatus(customerId, CustomerStatus.ACTIVE);
        logAction(adminId, adminName, "UNBLOCK", customerId, customer.getUserId(), "Compte débloqué");
    }

    @Override
    public void deleteCustomer(Long customerId, Long adminId, String adminName) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> CustomerNotFoundException.byId(customerId));
        long userId = customer.getUserId();
        try {
            authServiceClient.changeUserStatus(userId, "DELETED");
        } catch (Exception e) {
            log.warn("Auth change user status to DELETED failed for userId {}: {}", userId, e.getMessage());
        }
        customer.setStatus(CustomerStatus.DELETED);
        customerRepository.save(customer);
        logAction(adminId, adminName, "DELETE", customerId, userId, "Compte supprimé");
    }

    @Override
    public void sendResetPasswordEmail(Long customerId, Long adminId, String adminName) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> CustomerNotFoundException.byId(customerId));
        authServiceClient.sendResetPasswordEmail(customer.getUserId());
        logAction(adminId, adminName, "RESET_PASSWORD", customerId, customer.getUserId(), "Email de réinitialisation envoyé");
    }

    @Override
    public void sendNotification(Long customerId, String subject, String body, Long adminId, String adminName) {
        CustomerDTO dto = customerService.getCustomerById(customerId);
        String email = dto.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Client sans email");
        }
        notificationServiceClient.sendEmailToUser(NotificationServiceClient.SendEmailToUserRequest.builder()
                .email(email)
                .subject(subject)
                .body(body != null ? body : "")
                .build());
        logAction(adminId, adminName, "SEND_NOTIFICATION", customerId, dto.getUserId(), "Notification envoyée");
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCustomers(String format, CustomerStatus status, String search,
                                  LocalDateTime dateFrom, LocalDateTime dateTo) {
        if ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
            return exportExcel(status, search, dateFrom, dateTo);
        }
        return exportCsv(status, search, dateFrom, dateTo);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getNewCustomersByMonth(int year) {
        LocalDateTime start = Year.of(year).atDay(1).atStartOfDay();
        LocalDateTime end = Year.of(year).atMonth(12).atEndOfMonth().atTime(23, 59, 59);
        List<Customer> list = customerRepository.findByCreatedAtBetween(start, end);
        Map<String, Long> byMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            byMonth.put(String.format("%d-%02d", year, m), 0L);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        list.forEach(c -> {
            if (c.getCreatedAt() != null) {
                String key = c.getCreatedAt().format(formatter);
                byMonth.merge(key, 1L, Long::sum);
            }
        });
        return byMonth;
    }

    private void logAction(Long adminId, String adminName, String action, Long customerId, Long userId, String details) {
        if (adminId == null) adminId = 0L;
        if (adminName == null || adminName.isBlank()) adminName = "System";
        try {
            activityLogService.createLog(ActivityLogRequest.builder()
                    .adminId(adminId)
                    .adminName(adminName)
                    .action(action)
                    .resource("customers")
                    .resourceId(String.valueOf(customerId))
                    .details(details + " (userId=" + userId + ")")
                    .build());
        } catch (Exception e) {
            log.warn("Failed to create activity log: {}", e.getMessage());
        }
    }

    private byte[] exportCsv(CustomerStatus status, String search, LocalDateTime dateFrom, LocalDateTime dateTo) {
        LocalDateTime from = dateFrom != null ? dateFrom : DATE_FILTER_MIN;
        LocalDateTime to = dateTo != null ? dateTo : DATE_FILTER_MAX;
        List<Long> customerIdsFilter = resolveSearchFilter(search);
        if (customerIdsFilter != null && customerIdsFilter.isEmpty()) {
            return "id;userId;email;firstName;lastName;phoneNumber;status;city;createdAt;totalOrders;totalSpent\n".getBytes(StandardCharsets.UTF_8);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("id;userId;email;firstName;lastName;phoneNumber;status;city;createdAt;totalOrders;totalSpent\n");
        Pageable pageable = PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        int page = 0;
        while (page < EXPORT_MAX_PAGES) {
            Page<Customer> p = customerRepository.findWithFilters(status, customerIdsFilter, from, to, pageable);
            List<Customer> content = p.getContent();
            if (content.isEmpty()) break;
            for (Customer c : content) {
                CustomerDTO dto = customerService.getCustomerById(c.getId());
                String cityVal = dto.getDefaultAddress() != null ? dto.getDefaultAddress().getCity() : "";
                if (cityVal == null) cityVal = "";
                String createdAt = dto.getCreatedAt() != null ? dto.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "";
                sb.append(escapeCsv(dto.getId())).append(";")
                  .append(escapeCsv(dto.getUserId())).append(";")
                  .append(escapeCsv(dto.getEmail())).append(";")
                  .append(escapeCsv(dto.getFirstName())).append(";")
                  .append(escapeCsv(dto.getLastName())).append(";")
                  .append(escapeCsv(dto.getPhoneNumber())).append(";")
                  .append(escapeCsv(dto.getStatus() != null ? dto.getStatus().name() : "")).append(";")
                  .append(escapeCsv(cityVal)).append(";")
                  .append(escapeCsv(createdAt)).append(";")
                  .append(escapeCsv(dto.getTotalOrders() != null ? dto.getTotalOrders().toString() : "0")).append(";")
                  .append(escapeCsv(dto.getTotalSpent() != null ? dto.getTotalSpent().toString() : "0")).append("\n");
            }
            if (!p.hasNext()) break;
            page++;
            pageable = PageRequest.of(page, EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(Object o) {
        if (o == null) return "";
        String s = o.toString();
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private byte[] exportExcel(CustomerStatus status, String search, LocalDateTime dateFrom, LocalDateTime dateTo) {
        LocalDateTime from = dateFrom != null ? dateFrom : DATE_FILTER_MIN;
        LocalDateTime to = dateTo != null ? dateTo : DATE_FILTER_MAX;
        List<Long> customerIdsFilter = resolveSearchFilter(search);
        if (customerIdsFilter != null && customerIdsFilter.isEmpty()) {
            try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                wb.createSheet("Clients").createRow(0);
                wb.write(out);
                return out.toByteArray();
            } catch (Exception e) {
                return new byte[0];
            }
        }
        String[] headers = {"id", "userId", "email", "firstName", "lastName", "phoneNumber", "status", "city", "createdAt", "totalOrders", "totalSpent"};
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Clients");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            int rowNum = 1;
            Pageable pageable = PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
            int page = 0;
            while (page < EXPORT_MAX_PAGES) {
                Page<Customer> p = customerRepository.findWithFilters(status, customerIdsFilter, from, to, pageable);
                List<Customer> content = p.getContent();
                if (content.isEmpty()) break;
                for (Customer c : content) {
                    CustomerDTO dto = customerService.getCustomerById(c.getId());
                    Row row = sheet.createRow(rowNum++);
                    String cityVal = dto.getDefaultAddress() != null ? dto.getDefaultAddress().getCity() : "";
                    if (cityVal == null) cityVal = "";
                    String createdAt = dto.getCreatedAt() != null ? dto.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "";
                    row.createCell(0).setCellValue(dto.getId() != null ? dto.getId().doubleValue() : 0);
                    row.createCell(1).setCellValue(dto.getUserId() != null ? dto.getUserId().doubleValue() : 0);
                    setCellValue(row.createCell(2), dto.getEmail());
                    setCellValue(row.createCell(3), dto.getFirstName());
                    setCellValue(row.createCell(4), dto.getLastName());
                    setCellValue(row.createCell(5), dto.getPhoneNumber());
                    setCellValue(row.createCell(6), dto.getStatus() != null ? dto.getStatus().name() : "");
                    setCellValue(row.createCell(7), cityVal);
                    setCellValue(row.createCell(8), createdAt);
                    row.createCell(9).setCellValue(dto.getTotalOrders() != null ? dto.getTotalOrders().doubleValue() : 0);
                    row.createCell(10).setCellValue(dto.getTotalSpent() != null ? dto.getTotalSpent().doubleValue() : 0.0);
                }
                if (!p.hasNext()) break;
                page++;
                pageable = PageRequest.of(page, EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erreur export Excel clients", e);
            throw new RuntimeException("Erreur lors de la génération du fichier Excel", e);
        }
    }

    private void setCellValue(Cell cell, String value) {
        cell.setCellValue(value != null ? value : "");
    }
}
