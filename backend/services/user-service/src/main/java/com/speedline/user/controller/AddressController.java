package com.speedline.user.controller;

import com.speedline.user.dto.AddressDTO;
import com.speedline.user.dto.AddressUpdateRequest;
import com.speedline.user.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour la gestion des adresses de livraison
 * 
 * Endpoints:
 * - GET    /addresses/{id}  → récupérer une adresse par ID
 * - PUT    /addresses/{id}  → mettre à jour une adresse
 * - DELETE /addresses/{id}  → supprimer une adresse (soft delete)
 * 
 * Note: La création d'adresse est gérée via CustomerController (/customers/{id}/addresses)
 */
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AddressController implements IAddressController {

    private final AddressService addressService;

    // ==================== OPÉRATIONS CRUD ====================

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long id) {
        log.debug("GET /addresses/{} - Récupération de l'adresse", id);
        AddressDTO address = addressService.getAddressById(id);
        return ResponseEntity.ok(address);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<AddressDTO> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressUpdateRequest request) {
        log.info("PUT /addresses/{} - Mise à jour de l'adresse", id);
        AddressDTO updatedAddress = addressService.updateAddress(id, request);
        return ResponseEntity.ok(updatedAddress);
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        log.info("DELETE /addresses/{} - Suppression de l'adresse", id);
        addressService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }
}
