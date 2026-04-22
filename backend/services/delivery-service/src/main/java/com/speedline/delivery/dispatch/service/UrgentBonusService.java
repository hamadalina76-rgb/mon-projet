package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.client.PaymentServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrgentBonusService {

    private final PaymentServiceClient paymentServiceClient;
    private final DispatchProperties properties;

    public void creditBonus(Long courierId, Long orderId) {
        if (courierId == null || orderId == null) {
            return;
        }
        try {
            paymentServiceClient.creditBonus(
                    courierId,
                    new PaymentServiceClient.BonusRequest(
                            BigDecimal.valueOf(properties.getUrgent().getBonusAmount()),
                            "Mission urgente",
                            "urgent-delivery:" + orderId
                    )
            );
        } catch (Exception ex) {
            log.warn("Urgent bonus credit failed courierId={} orderId={}: {}",
                    courierId, orderId, ex.getMessage());
        }
    }
}
