package com.speedline.delivery.controller;

import com.speedline.delivery.domain.DeliveryStatus;
import com.speedline.delivery.dto.DeliveryDTO;
import com.speedline.delivery.service.DeliveryService;
import com.speedline.delivery.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * API REST du cycle de vie d'une livraison (hors /deliveries/couriers/* géré par
 * {@link CourierDeliveryStatusController}).
 */
@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
@Tag(name = "Deliveries", description = "Cycle de vie des livraisons")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final TrackingService trackingService;

    @Operation(summary = "Créer une livraison (manuel ou intégration)")
    @PostMapping
    public ResponseEntity<DeliveryDTO> create(@RequestBody @Valid CreateDeliveryRequest body) {
        DeliveryDTO dto = deliveryService.createDelivery(
                body.getOrderId(),
                body.getOrderNumber(),
                body.getCustomerName(),
                body.getCustomerPhone(),
                body.getPartnerName(),
                body.getPickupLat(),
                body.getPickupLon(),
                body.getPickupAddress(),
                body.getDropoffLat(),
                body.getDropoffLon(),
                body.getDropoffAddress(),
                body.getDeliveryInstructions(),
                body.getDeliveryFee(),
                body.getAssignedCourierId(),
                body.getBundleId(),
                body.getEtaPickupMin(),
                body.getEtaDeliveryMin());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Détail livraison")
    @GetMapping("/{id}")
    public DeliveryDTO get(@PathVariable Long id) {
        return deliveryService.getDeliveryById(id);
    }

    @Operation(summary = "Livraison par commande")
    @GetMapping("/by-order/{orderId}")
    public DeliveryDTO getByOrder(@PathVariable Long orderId) {
        return deliveryService.getDeliveryByOrderId(orderId);
    }

    @Operation(summary = "Livraisons d'un livreur")
    @GetMapping("/courier/{courierId}")
    public Page<DeliveryDTO> listByCourier(
            @PathVariable Long courierId,
            @ParameterObject Pageable pageable) {
        return deliveryService.getCourierDeliveries(courierId, pageable);
    }

    @Operation(summary = "Livraisons actives d'un livreur")
    @GetMapping("/courier/{courierId}/active")
    public List<DeliveryDTO> activeByCourier(@PathVariable Long courierId) {
        return deliveryService.getActiveCourierDeliveries(courierId);
    }

    @Operation(summary = "Par statut")
    @GetMapping("/status/{status}")
    public Page<DeliveryDTO> byStatus(
            @PathVariable DeliveryStatus status,
            @ParameterObject Pageable pageable) {
        return deliveryService.getDeliveriesByStatus(status, pageable);
    }

    @Operation(summary = "Livraisons en attente d'assignation")
    @GetMapping("/pending")
    public List<DeliveryDTO> pending() {
        return deliveryService.getPendingDeliveries();
    }

    @Operation(summary = "Accepter")
    @PutMapping("/{id}/accept")
    public DeliveryDTO accept(
            @PathVariable Long id,
            @RequestParam @NotNull Long courierId) {
        return deliveryService.acceptDelivery(id, courierId);
    }

    @Operation(summary = "Refuser")
    @PutMapping("/{id}/decline")
    public DeliveryDTO decline(
            @PathVariable Long id,
            @RequestParam @NotNull Long courierId,
            @RequestParam(required = false) String reason) {
        return deliveryService.declineDelivery(id, courierId, reason);
    }

    @Operation(summary = "Accepter par orderId (courier app)")
    @PutMapping("/by-order/{orderId}/accept")
    public DeliveryDTO acceptByOrder(@PathVariable Long orderId, @RequestParam @NotNull Long courierId) {
        return deliveryService.acceptDeliveryByOrderId(orderId, courierId);
    }

    @Operation(summary = "Refuser par orderId (courier app)")
    @PutMapping("/by-order/{orderId}/decline")
    public DeliveryDTO declineByOrder(
            @PathVariable Long orderId,
            @RequestParam @NotNull Long courierId,
            @RequestParam(required = false) String reason) {
        return deliveryService.declineDeliveryByOrderId(orderId, courierId, reason);
    }

    @Operation(summary = "Arrivée au pickup")
    @PutMapping("/{id}/arrived-pickup")
    public DeliveryDTO arrivedPickup(@PathVariable Long id, @RequestParam @NotNull Long courierId) {
        return deliveryService.arrivedAtPickup(id, courierId);
    }

    @Operation(summary = "Commande récupérée")
    @PutMapping("/{id}/pickup")
    public DeliveryDTO pickedUp(@PathVariable Long id, @RequestParam @NotNull Long courierId) {
        return deliveryService.pickupOrder(id, courierId);
    }

    @Operation(summary = "Début du trajet client")
    @PutMapping("/{id}/start")
    public DeliveryDTO start(@PathVariable Long id, @RequestParam @NotNull Long courierId) {
        return deliveryService.startDelivery(id, courierId);
    }

    @Operation(summary = "Arrivée chez le client")
    @PutMapping("/{id}/arrived-dropoff")
    public DeliveryDTO arrivedDropoff(@PathVariable Long id, @RequestParam @NotNull Long courierId) {
        return deliveryService.arrivedAtDropoff(id, courierId);
    }

    @Operation(summary = "Terminer (preuve + notes optionnelles)")
    @PutMapping("/{id}/complete")
    public DeliveryDTO complete(
            @PathVariable Long id,
            @RequestParam @NotNull Long courierId,
            @RequestParam(required = false) String proofImageUrl,
            @RequestParam(required = false) String notes) {
        return deliveryService.completeDelivery(id, courierId, proofImageUrl, notes);
    }

    @Operation(summary = "Annuler")
    @PutMapping("/{id}/cancel")
    public DeliveryDTO cancel(
            @PathVariable Long id,
            @RequestBody @Valid CancelDeliveryRequest body) {
        return deliveryService.cancelDelivery(id, body.getCancelledBy(), body.getReason());
    }

    @Operation(summary = "Echec")
    @PutMapping("/{id}/fail")
    public DeliveryDTO fail(
            @PathVariable Long id,
            @RequestParam @NotNull Long courierId,
            @RequestParam(required = false) String reason) {
        return deliveryService.failDelivery(id, courierId, reason);
    }

    @Operation(summary = "Ajouter un pourboire")
    @PostMapping("/{id}/tip")
    public DeliveryDTO tip(
            @PathVariable Long id,
            @RequestParam @NotNull BigDecimal amount) {
        return deliveryService.addTip(id, amount);
    }

    @Operation(summary = "Dernière position (tracking point ou Redis)")
    @GetMapping("/{id}/track/last")
    public ResponseEntity<com.speedline.delivery.dto.TrackingPointDTO> lastTrack(
            @PathVariable Long id) {
        var p = trackingService.getLastLocation(id);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(p);
    }

    @Data
    public static class CreateDeliveryRequest {
        @NotNull
        private Long orderId;
        private String orderNumber;
        private String customerName;
        private String customerPhone;
        private String partnerName;
        private BigDecimal pickupLat;
        private BigDecimal pickupLon;
        private String pickupAddress;
        private BigDecimal dropoffLat;
        private BigDecimal dropoffLon;
        private String dropoffAddress;
        private String deliveryInstructions;
        private BigDecimal deliveryFee;
        private Long assignedCourierId;
        private Long bundleId;
        private Integer etaPickupMin;
        private Integer etaDeliveryMin;
    }

    @Data
    public static class CancelDeliveryRequest {
        @NotNull
        private String cancelledBy;
        private String reason;
    }
}
