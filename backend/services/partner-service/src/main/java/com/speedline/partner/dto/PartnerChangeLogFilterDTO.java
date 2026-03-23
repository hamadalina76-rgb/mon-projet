package com.speedline.partner.dto;

import lombok.Data;

@Data
public class PartnerChangeLogFilterDTO {
    private int page = 0;
    private int size = 10;
    private String action;
    private Long adminId;
    private String adminFullName;
    private String dateFrom;   // format ISO : "2025-01-01"
    private String dateTo;
    private String changedField;
}