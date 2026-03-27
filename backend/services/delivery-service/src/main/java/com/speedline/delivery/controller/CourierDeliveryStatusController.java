package com.speedline.delivery.controller;

import com.speedline.delivery.repository.DeliveryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Courier Delivery Status", description = "Etat des livraisons actives d'un livreur")
public class CourierDeliveryStatusController {

    private final DeliveryRepository deliveryRepository;

    @Operation(summary = "Verifier si un livreur a une livraison active")
    @GetMapping("/couriers/{courierId}/active-status")
    public ResponseEntity<Map<String, Object>> getCourierActiveDeliveryStatus(
            @PathVariable Long courierId
    ) {
        var activeDeliveries = deliveryRepository.findActiveDeliveriesByCourier(courierId);
        boolean hasActiveDelivery = !activeDeliveries.isEmpty();

        log.debug("Courier {} active deliveries count={}", courierId, activeDeliveries.size());

        return ResponseEntity.ok(Map.of(
                "courierId", courierId,
                "hasActiveDelivery", hasActiveDelivery,
                "activeCount", activeDeliveries.size()
        ));
    }
}
