import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:geocoding/geocoding.dart' as geo;
import '../../../../config/runtime_config.dart';
import '../models/saved_location.dart';

/// Service API pour la géolocalisation
/// Communique avec le location-service via l'API Gateway
class LocationApiService {
  final Dio _dio;

  LocationApiService({Dio? dio})
      : _dio = dio ??
            Dio(BaseOptions(
              baseUrl: RuntimeConfig.apiBaseUrl,
              connectTimeout: const Duration(seconds: 3),  // fail fast → Nominatim
              receiveTimeout: const Duration(seconds: 10),
              headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
              },
            ));

  /// Géocodage inverse: coordonnées GPS → adresse
  ///
  /// Appelle POST /api/locations/reverse-geocode
  /// Retourne un [SavedLocation] avec l'adresse détectée
  Future<SavedLocation> reverseGeocode({
    required double latitude,
    required double longitude,
  }) async {
    try {
      debugPrint('[Geocode] ► reverseGeocode lat=$latitude lon=$longitude (POST /api/locations/reverse-geocode)');
      final response = await _dio.post(
        '/api/locations/reverse-geocode',
        data: {
          'latitude': latitude,
          'longitude': longitude,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data as Map<String, dynamic>;
        final rawAddress = data['formattedAddress'] as String? ?? '';

        // Si le backend retourne une adresse vide ou des coords brutes,
        // utiliser Nominatim en fallback
        if (rawAddress.isEmpty || looksLikeCoordinates(rawAddress)) {
          debugPrint('[Geocode] Backend returned empty/coords → native/Nominatim fallback');
          return _geocodeFallback(latitude: latitude, longitude: longitude);
        }

        debugPrint('[Geocode] ✔ Backend address: "$rawAddress"');
        return SavedLocation(
          latitude: (data['latitude'] as num?)?.toDouble() ?? latitude,
          longitude: (data['longitude'] as num?)?.toDouble() ?? longitude,
          formattedAddress: rawAddress,
          street: data['street'] as String? ?? '',
          city: data['city'] as String? ?? '',
          state: data['state'] as String? ?? '',
          postalCode: data['postalCode'] as String? ?? '',
          country: data['country'] as String? ?? 'Tunisie',
          savedAt: DateTime.now(),
        );
      }

      throw Exception('Réponse invalide du serveur: ${response.statusCode}');
    } on DioException catch (e) {
      // Backend unreachable for any reason → fall through to native/Nominatim
      debugPrint('[Geocode] Backend unreachable (${e.type.name}) → native/Nominatim fallback');
      return _geocodeFallback(latitude: latitude, longitude: longitude);
    } catch (_) {
      // Non-DioException (e.g. bad status code path) → also use fallback
      return _geocodeFallback(latitude: latitude, longitude: longitude);
    }
  }

  /// Géocodage direct (forward): texte d'adresse → liste de résultats.
  /// Utilise l'API Nominatim/OpenStreetMap directement.
  Future<List<SavedLocation>> forwardGeocode(String query) async {
    if (query.trim().isEmpty) return [];
    try {
      debugPrint('[ForwardGeocode] ► "$query"');
      final nominatim = Dio(BaseOptions(
        baseUrl: 'https://nominatim.openstreetmap.org',
        connectTimeout: const Duration(seconds: 8),
        receiveTimeout: const Duration(seconds: 8),
        headers: {
          'User-Agent': 'SpeedLine-Customer-App/1.0',
          'Accept-Language': 'fr,en',
          'Accept': 'application/json',
        },
      ));
      final resp = await nominatim.get<List<dynamic>>(
        '/search',
        queryParameters: {
          'q': query.trim(),
          'format': 'json',
          'addressdetails': 1,
          'limit': 5,
        },
      );
      final list = resp.data;
      if (list == null || list.isEmpty) return [];

      return list.map((e) {
        final data = e as Map<String, dynamic>;
        final addr = data['address'] as Map<String, dynamic>? ?? {};
        final lat = double.tryParse(data['lat'] as String? ?? '') ?? 0.0;
        final lon = double.tryParse(data['lon'] as String? ?? '') ?? 0.0;
        final road = addr['road']         as String?
            ?? addr['pedestrian']         as String?
            ?? addr['path']               as String?
            ?? '';
        final city = addr['city']         as String?
            ?? addr['town']               as String?
            ?? addr['village']            as String?
            ?? addr['county']             as String?
            ?? '';
        final state    = addr['state']    as String? ?? '';
        final postcode = addr['postcode'] as String? ?? '';
        final country  = addr['country']  as String? ?? '';
        final display  = data['display_name'] as String? ?? '';
        // Build a shorter label: "road, city, country"
        final shortParts = <String>[];
        if (road.isNotEmpty) shortParts.add(road);
        if (city.isNotEmpty) shortParts.add(city);
        if (country.isNotEmpty) shortParts.add(country);
        final formatted = shortParts.isNotEmpty ? shortParts.join(', ') : display;
        debugPrint('[ForwardGeocode] ◄ $formatted ($lat, $lon)');
        return SavedLocation(
          latitude: lat,
          longitude: lon,
          formattedAddress: formatted,
          street: road,
          city: city,
          state: state,
          postalCode: postcode,
          country: country,
          savedAt: DateTime.now(),
        );
      }).toList();
    } catch (e) {
      debugPrint('[ForwardGeocode] ✗ $e');
      return [];
    }
  }

  /// Récupère les partenaires proches via l'API
  /// GET /api/locations/nearby-partners?lat=..&lon=..&radius=..
  ///
  /// En cas d'erreur retourne une liste vide (ne crash pas l'app).
  Future<List<Map<String, dynamic>>> getNearbyPartners({
    required double latitude,
    required double longitude,
    int radiusMeters = 5000,
  }) async {
    try {
      debugPrint('[PostGIS] ► getNearbyPartners lat=$latitude lon=$longitude radius=${radiusMeters}m');

      final response = await _dio.get(
        '/api/locations/nearby-partners',
        queryParameters: {
          'lat': latitude,
          'lon': longitude,
          'radius': radiusMeters,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        final list = (response.data as List<dynamic>)
            .map((e) => Map<String, dynamic>.from(e as Map<String, dynamic>))
            .toList();

        // ── Console output so you can verify PostGIS distances ──
        debugPrint('[PostGIS] ◄ ${list.length} partner(s) found within ${radiusMeters}m:');
        for (final p in list) {
          final dist  = (p['distanceKm'] as num?)?.toStringAsFixed(3) ?? '?';
          final eta   = p['etaMinutes'] ?? '?';
          final name  = p['name'] ?? 'Unknown';
          final id    = p['partnerId'] ?? '?';
          debugPrint('[PostGIS]   id=$id "$name" → ${dist}km (≈${eta}min)');
        }
        return list;
      }

      debugPrint('[PostGIS] ⚠ Unexpected status: ${response.statusCode}');
      return [];
    } on DioException catch (e) {
      debugPrint('[PostGIS] ❌ DioException in getNearbyPartners: ${e.message}');
      return [];
    } catch (e) {
      debugPrint('[PostGIS] ❌ Unexpected error in getNearbyPartners: $e');
      return [];
    }
  }

  // ─── Fallback chain ────────────────────────────────────────────────────────

  /// Fallback 1 (fast, offline): Android / iOS native geocoder.
  /// Uses Google's on-device geocoder on Android — always works on emulators.
  Future<SavedLocation?> _nativeGeocode({
    required double latitude,
    required double longitude,
  }) async {
    try {
      debugPrint('[Geocode] ► Native geocoder lat=$latitude lon=$longitude');
      final placemarks = await geo.placemarkFromCoordinates(latitude, longitude);
      if (placemarks.isEmpty) return null;

      final p = placemarks.first;
      final road     = p.thoroughfare    ?? p.subThoroughfare ?? '';
      final suburb   = p.subLocality     ?? '';
      final city     = p.locality        ?? p.administrativeArea ?? '';
      final state    = p.administrativeArea ?? '';
      final postcode = p.postalCode       ?? '';
      final country  = p.country          ?? '';

      final parts = <String>[];
      if (road.isNotEmpty)   parts.add(road);
      if (suburb.isNotEmpty && city.isEmpty) parts.add(suburb);
      if (city.isNotEmpty)   parts.add(city);
      final formatted = parts.isNotEmpty ? parts.join(', ') : '$latitude, $longitude';

      debugPrint('[Geocode] ✔ Native result: "$formatted"');
      return SavedLocation(
        latitude: latitude,
        longitude: longitude,
        formattedAddress: formatted,
        street: road,
        city: city,
        state: state,
        postalCode: postcode,
        country: country,
        savedAt: DateTime.now(),
      );
    } catch (e) {
      debugPrint('[Geocode] ✗ Native geocoder failed: $e');
      return null;
    }
  }

  /// Fallback 2: Nominatim (OpenStreetMap) — needs internet.
  Future<SavedLocation?> _nominatimGeocode({
    required double latitude,
    required double longitude,
  }) async {
    try {
      debugPrint('[Geocode] ► Nominatim lat=$latitude lon=$longitude');
      final nominatim = Dio(BaseOptions(
        baseUrl: 'https://nominatim.openstreetmap.org',
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
        headers: {
          'User-Agent': 'SpeedLine-Customer-App/1.0',
          'Accept-Language': 'fr,en',
          'Accept': 'application/json',
        },
      ));

      final resp = await nominatim.get<Map<String, dynamic>>(
        '/reverse',
        queryParameters: {
          'lat': latitude,
          'lon': longitude,
          'format': 'json',
          'addressdetails': 1,
        },
      );

      final data = resp.data;
      if (data == null) return null;

      final addr = data['address'] as Map<String, dynamic>? ?? {};

      final road    = addr['road']         as String?
          ?? addr['pedestrian']            as String?
          ?? addr['path']                  as String?
          ?? '';
      final suburb  = addr['suburb']       as String?
          ?? addr['neighbourhood']         as String?
          ?? '';
      final city    = addr['city']         as String?
          ?? addr['town']                  as String?
          ?? addr['village']               as String?
          ?? addr['county']                as String?
          ?? '';
      final state    = addr['state']      as String? ?? '';
      final postcode = addr['postcode']   as String? ?? '';
      final country  = addr['country']    as String? ?? '';
      final displayName = data['display_name'] as String? ?? '';

      final parts = <String>[];
      if (road.isNotEmpty) parts.add(road);
      if (suburb.isNotEmpty && city.isEmpty) parts.add(suburb);
      if (city.isNotEmpty) parts.add(city);
      final formatted = parts.isNotEmpty ? parts.join(', ') : displayName;

      debugPrint('[Geocode] ✔ Nominatim result: "$formatted"');
      return SavedLocation(
        latitude: latitude,
        longitude: longitude,
        formattedAddress: formatted,
        street: road,
        city: city,
        state: state,
        postalCode: postcode,
        country: country,
        savedAt: DateTime.now(),
      );
    } catch (e) {
      debugPrint('[Geocode] ✗ Nominatim failed: $e');
      return null;
    }
  }

  /// Master fallback: tries native → Nominatim → raw coordinates.
  Future<SavedLocation> _geocodeFallback({
    required double latitude,
    required double longitude,
  }) async {
    // 1️⃣  Native platform geocoder (fastest, no network needed on emulator)
    final native = await _nativeGeocode(latitude: latitude, longitude: longitude);
    if (native != null) return native;

    // 2️⃣  Nominatim over the internet
    final nominatim = await _nominatimGeocode(latitude: latitude, longitude: longitude);
    if (nominatim != null) return nominatim;

    // 3️⃣  Last resort: human-readable coordinates
    debugPrint('[Geocode] ⚠ All geocoding failed → raw coordinate display');
    final ns = latitude  >= 0 ? 'N' : 'S';
    final ew = longitude >= 0 ? 'E' : 'O';
    return SavedLocation(
      latitude: latitude,
      longitude: longitude,
      formattedAddress:
          '${latitude.abs().toStringAsFixed(4)}°$ns, '
          '${longitude.abs().toStringAsFixed(4)}°$ew',
      savedAt: DateTime.now(),
    );
  }

  /// Détecte si une chaîne ressemble à des coordonnées brutes plutôt qu'une adresse.
  /// Exemples détectés :
  ///   "36.8054°N, 10.1828°E"   (format dernier recours avec cardinal)
  ///   "36.4219983, -122.084"   (décimaux bruts)
  ///   "36.467°N, 10.802°E"     (degré seul)
  static bool looksLikeCoordinates(String s) {
    final t = s.trim();
    // Matches: optional minus, digits, optional decimal part, optional degree sign,
    // optional space, optional cardinal (N/S/E/O/W), then comma-space, then same.
    return RegExp(
      r'^-?\d+(\.\d+)?\s*°?\s*[NSns]?\s*,\s*-?\d+(\.\d+)?\s*°?\s*[EeOoWw]?\s*$',
    ).hasMatch(t);
  }
}
