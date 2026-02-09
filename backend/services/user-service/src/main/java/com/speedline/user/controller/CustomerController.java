package com.speedline.user.controller;

import com.speedline.user.dto.*;
import com.speedline.user.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller pour la gestion des clients (Customers)
 * 
 * Fournit les endpoints pour :
 * - Consultation, mise à jour et suppression des profils clients
 * - Gestion des adresses
 * - Gestion des partenaires favoris
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CustomerController implements ICustomerController {

    private final CustomerService customerService;

    // ==================== OPÉRATIONS CLIENT ====================

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        log.debug("GET /customers/{} - Récupération du client", id);
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request) {
        log.info("PUT /customers/{} - Mise à jour du profil", id);
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.info("DELETE /customers/{} - Suppression du client", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== GESTION DES ADRESSES ====================

    @GetMapping("/{id}/addresses")
    @Override
    public ResponseEntity<List<AddressDTO>> getCustomerAddresses(@PathVariable Long id) {
        log.debug("GET /customers/{}/addresses - Récupération des adresses", id);
        return ResponseEntity.ok(customerService.getCustomerAddresses(id));
    }

    @PostMapping("/{id}/addresses")
    @Override
    public ResponseEntity<AddressDTO> createAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressCreateRequest request) {
        log.info("POST /customers/{}/addresses - Création d'une nouvelle adresse", id);
        AddressDTO address = customerService.createAddress(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    // ==================== GESTION DES FAVORIS ====================

    @GetMapping("/{id}/favorites")
    @Override
    public ResponseEntity<List<Long>> getFavoritePartners(@PathVariable Long id) {
        log.debug("GET /customers/{}/favorites - Récupération des partenaires favoris", id);
        return ResponseEntity.ok(customerService.getFavoritePartnerIds(id));
    }

    @PostMapping("/{id}/favorites/{partnerId}")
    @Override
    public ResponseEntity<Void> addFavoritePartner(
            @PathVariable Long id,
            @PathVariable Long partnerId) {
        log.info("POST /customers/{}/favorites/{} - Ajout aux favoris", id, partnerId);
        customerService.addFavoritePartner(id, partnerId);
        return ResponseEntity.ok().build();
    }
}
