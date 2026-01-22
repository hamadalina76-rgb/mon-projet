package com.speedline.location.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Location
 * 
 * Endpoints:
 * GET    /locations/nearby-partners - Partenaires proches
 * POST   /locations/calculate-distance - Calculer distance
 * POST   /locations/geocode - Adresse → Coordonnées
 * POST   /locations/reverse-geocode - Coordonnées → Adresse
 * GET    /locations/route - Calculer itinéraire
 */
@RestController
@RequestMapping("/locations")
public class LocationController {
    // TODO: Implémenter
}
