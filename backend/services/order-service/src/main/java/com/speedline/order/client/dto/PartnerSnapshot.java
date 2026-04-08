package com.speedline.order.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartnerSnapshot {

    private Long id;
    /** Compte utilisateur lié au partenaire (dashboard partenaire = ce userId pour les notifications). */
    private Long userId;
    private String businessName;
    private String name;
    private String address;
    private String phoneNumber;

    private Boolean acceptsOrders;
    private Boolean isCurrentlyOpen;

    private BigDecimal deliveryFee;
    private BigDecimal minimumOrder;
    private BigDecimal serviceFee;
    private BigDecimal commissionRate;

    private Integer preparationTime;
}
