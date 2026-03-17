package com.speedline.location.controller;

import com.speedline.location.dto.CourierPositionDto;
import com.speedline.location.dto.NearbyCourierDto;
import com.speedline.location.service.CourierPositionRedisService;
import com.speedline.location.service.NearbyCourierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations/couriers")
@RequiredArgsConstructor
@Tag(
        name = "Courier Locations",
        description = "API de positions temps réel des livreurs (nearby, position actuelle, liste online)."
)
public class CourierLocationController {

    private final NearbyCourierService nearbyCourierService;
    private final CourierPositionRedisService positionRedisService;

    @GetMapping("/nearby")
    @Operation(
            summary = "Livreurs en ligne à proximité d'un point",
            description = """
                    Retourne les livreurs actuellement en ligne dans un rayon donné autour d'un point (lat,lng),
                    triés par distance croissante.

                    - TC-15: lat/lng + radiusKm=2 → livreurs dans ce rayon, triés par distance
                    - TC-18: les livreurs hors ligne ne sont pas inclus
                    """
    )
    public ResponseEntity<List<NearbyCourierDto>> getNearbyCouriers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(name = "radiusKm") double radiusKm
    ) {
        List<NearbyCourierDto> result = nearbyCourierService.findNearby(lat, lng, radiusKm);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{courierId}/position")
    @Operation(
            summary = "Position actuelle d'un livreur",
            description = """
                    Lit la dernière position connue du livreur dans Redis.

                    - Retourne 200 + position si trouvée
                    - Retourne 404 si aucune position n'est disponible (TTL expiré / jamais vue)
                    - TC-17: réponse attendue < 10 ms pour une clé présente en mémoire
                    """
    )
    public ResponseEntity<CourierPositionDto> getCourierPosition(@PathVariable String courierId) {
        return positionRedisService.getPosition(courierId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/online")
    @Operation(
            summary = "Liste des livreurs actuellement en ligne",
            description = """
                    Liste tous les livreurs actuellement en ligne (présents dans Redis avec une clé isOnline non expirée).

                    - TC-16: si aucun livreur en ligne → [] et HTTP 200
                    """
    )
    public ResponseEntity<List<CourierPositionDto>> getOnlineCouriers() {
        List<CourierPositionDto> all = positionRedisService.getAllOnline();
        return ResponseEntity.ok(all);
    }
}

