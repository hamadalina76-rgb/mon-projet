package com.speedline.delivery.dispatch.util;

public final class GeoUtil {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoUtil() {
    }

    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    public static int travelMinutes(double meters, double speedKmh) {
        if (speedKmh <= 0.0 || meters <= 0.0) {
            return 0;
        }
        double km = meters / 1000.0;
        double hours = km / speedKmh;
        return (int) Math.ceil(hours * 60.0);
    }

    /**
     * Extra meters compared to going directly from courier to destination.
     */
    public static double detourMeters(double courierLat, double courierLon,
                                      double waypointLat, double waypointLon,
                                      double destinationLat, double destinationLon) {
        return haversineMeters(courierLat, courierLon, waypointLat, waypointLon)
                + haversineMeters(waypointLat, waypointLon, destinationLat, destinationLon)
                - haversineMeters(courierLat, courierLon, destinationLat, destinationLon);
    }

    public static double toMinutes(double meters, double speedMps) {
        if (meters <= 0.0 || speedMps <= 0.0) {
            return 0.0;
        }
        return (meters / speedMps) / 60.0;
    }
}
