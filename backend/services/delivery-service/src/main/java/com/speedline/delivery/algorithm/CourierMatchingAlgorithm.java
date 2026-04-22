package com.speedline.delivery.algorithm;

/**
 * Score heuristique de matching livreur (plus bas = mieux). Utilisable par des tests ou
 * outils d'analyse; le dispatch runtime utilise le moteur de coût et le solver.
 */
public final class CourierMatchingAlgorithm {

    private CourierMatchingAlgorithm() {
    }

    /**
     * @param distanceKm distance estimée en km
     * @param rating01 note normalisée 0.0 (mauvais) à 1.0 (excellent) ; si inconnu, utiliser 0.5
     * @param activeDeliveries nombre de courses actives du livreur
     * @param isOnline      si le livreur est en ligne / disponible
     */
    public static double score(
            double distanceKm,
            double rating01,
            int activeDeliveries,
            boolean isOnline) {
        double dist = Math.max(0, distanceKm) * 0.5;
        double load = Math.max(0, activeDeliveries) * 2.0;
        double r = (1.0 - clamp01(rating01)) * 5.0;
        double offline = isOnline ? 0.0 : 1_000.0;
        return dist + load + r + offline;
    }

    private static double clamp01(double v) {
        if (v < 0) {
            return 0;
        }
        if (v > 1) {
            return 1;
        }
        return v;
    }
}
