import 'dart:math' as math;

/// Converts delivery radius to kilometers.
///
/// Legacy payloads can send meters for values greater than 50.
double? normalizeDeliveryRadiusKm(int? deliveryRadius) {
  if (deliveryRadius == null) return null;
  final raw = deliveryRadius.toDouble();
  if (raw <= 0) return null;
  return raw > 50 ? raw / 1000.0 : raw;
}

/// Returns true when the destination is outside partner delivery area.
///
/// Priority:
/// 1) Use backend-provided [distanceKm] when available.
/// 2) Fallback to a coordinate-based distance calculation.
bool isOutsideDeliveryZone({
  required int? deliveryRadius,
  double? distanceKm,
  double? userLat,
  double? userLng,
  double? partnerLat,
  double? partnerLng,
}) {
  final radiusKm = normalizeDeliveryRadiusKm(deliveryRadius);
  if (radiusKm == null) return false;

  if (distanceKm != null && distanceKm >= 0) {
    return distanceKm > radiusKm;
  }

  if (userLat == null ||
      userLng == null ||
      partnerLat == null ||
      partnerLng == null) {
    return false;
  }

  final fallbackDistance = haversineDistanceKm(
    userLat: userLat,
    userLng: userLng,
    partnerLat: partnerLat,
    partnerLng: partnerLng,
  );

  return fallbackDistance > radiusKm;
}

/// Great-circle distance between two coordinates in kilometers.
double haversineDistanceKm({
  required double userLat,
  required double userLng,
  required double partnerLat,
  required double partnerLng,
}) {
  const earthRadiusKm = 6371.0;

  final dLat = _toRadians(partnerLat - userLat);
  final dLng = _toRadians(partnerLng - userLng);

  final a =
      math.sin(dLat / 2) * math.sin(dLat / 2) +
      math.cos(_toRadians(userLat)) *
          math.cos(_toRadians(partnerLat)) *
          math.sin(dLng / 2) *
          math.sin(dLng / 2);

  final c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a));
  return earthRadiusKm * c;
}

double _toRadians(double degrees) => degrees * (math.pi / 180.0);
