package com.speedline.delivery.controller;

import com.speedline.delivery.dto.DeliveryDTO;
import com.speedline.delivery.repository.DeliveryRepository;
import com.speedline.delivery.service.CourierMatchingService;
import com.speedline.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Courier Delivery Status", description = "Etat des livraisons actives d'un livreur")
public class CourierDeliveryStatusController {

    private final DeliveryRepository deliveryRepository;
    private final CourierMatchingService courierMatchingService;
    private final DeliveryService deliveryService;

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

    @Operation(summary = "Accepter une offre de livraison (courier app) — par orderId")
    @PutMapping("/by-order/{orderId}/accept")
    public ResponseEntity<DeliveryDTO> acceptByOrderId(
            @PathVariable Long orderId,
            @RequestParam @NotNull Long courierId) {
        return ResponseEntity.ok(deliveryService.acceptDeliveryByOrderId(orderId, courierId));
    }

    @Operation(summary = "Refuser une offre de livraison (courier app) — par orderId")
    @PutMapping("/by-order/{orderId}/decline")
    public ResponseEntity<DeliveryDTO> declineByOrderId(
            @PathVariable Long orderId,
            @RequestParam @NotNull Long courierId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(deliveryService.declineDeliveryByOrderId(orderId, courierId, reason));
    }

    @Operation(summary = "Reassigner les livraisons actives d'un livreur devenu indisponible")
    @PostMapping("/couriers/{courierId}/unavailable/reassign")
    public ResponseEntity<Map<String, Object>> reassignOnCourierUnavailability(
            @PathVariable Long courierId
    ) {
        Map<String, Object> result = courierMatchingService.reassignOnCourierUnavailability(courierId);
        return ResponseEntity.ok(result);
    }
}
