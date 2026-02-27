import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';
import '../../data/models/saved_location.dart';
import '../../data/services/location_api_service.dart';

/// Clé Hive pour stocker la localisation courante
const String _kLocationBoxName = 'location_box';
const String _kCurrentLocationKey = 'current_location';

// === Providers ===

/// Provider pour le service API location
final locationApiServiceProvider = Provider<LocationApiService>(
  (ref) => LocationApiService(),
);

/// Provider pour la localisation sauvegardée (state global)
final locationNotifierProvider =
    StateNotifierProvider<LocationNotifier, LocationState>(
  (ref) => LocationNotifier(ref.watch(locationApiServiceProvider)),
);

/// Provider pour lire la localisation depuis Hive
final savedLocationProvider = FutureProvider<SavedLocation?>((ref) async {
  final box = await Hive.openBox<String>(_kLocationBoxName);
  final json = box.get(_kCurrentLocationKey);
  if (json == null) return null;
  return SavedLocation.fromJson(json);
});

// === State ===

/// États possibles de la localisation
enum LocationStatus {
  initial,
  loading,       // Détection GPS en cours
  reversGeocoding, // Appel API en cours
  success,       // Localisation obtenue
  error,         // Erreur quelconque
  denied,        // Permission refusée → mode manuel
  serviceOff,    // GPS désactivé → mode manuel
}

/// État complet de la localisation
class LocationState {
  final LocationStatus status;
  final SavedLocation? location;
  final String? errorMessage;

  const LocationState({
    this.status = LocationStatus.initial,
    this.location,
    this.errorMessage,
  });

  bool get isLoading =>
      status == LocationStatus.loading ||
      status == LocationStatus.reversGeocoding;

  bool get hasLocation => location != null && status == LocationStatus.success;

  bool get needsManualInput =>
      status == LocationStatus.denied || status == LocationStatus.serviceOff;

  LocationState copyWith({
    LocationStatus? status,
    SavedLocation? location,
    String? errorMessage,
  }) {
    return LocationState(
      status: status ?? this.status,
      location: location ?? this.location,
      errorMessage: errorMessage,
    );
  }
}

// === Notifier ===

class LocationNotifier extends StateNotifier<LocationState> {
  final LocationApiService _apiService;

  LocationNotifier(this._apiService) : super(const LocationState()) {
    _loadFromHive();
  }

  /// Loads the previously saved location from Hive so state is populated on startup.
  Future<void> _loadFromHive() async {
    try {
      final box = await Hive.openBox<String>(_kLocationBoxName);
      final json = box.get(_kCurrentLocationKey);
      if (json != null) {
        final loc = SavedLocation.fromJson(json);
        state = state.copyWith(
          status: LocationStatus.success,
          location: loc,
        );
      }
    } catch (_) {
      // No persisted location — keep initial state
    }
  }

  /// Met à jour l'état avec une localisation et la sauvegarde dans Hive et le backend
  Future<void> confirmLocation(SavedLocation location) async {
    try {
      final box = await Hive.openBox<String>(_kLocationBoxName);
      await box.put(_kCurrentLocationKey, location.toJson());
      state = state.copyWith(
        status: LocationStatus.success,
        location: location,
      );
      // Persister aussi dans le backend (fire-and-forget).
      // Si le backend est injoignable, Hive garde l'adresse localement.
      unawaited(_apiService.saveLocationToBackend(location));
    } catch (e) {
      state = state.copyWith(
        status: LocationStatus.error,
        errorMessage: 'Erreur de sauvegarde: $e',
      );
    }
  }

  /// Effectue le géocodage inverse et met à jour l'état
  Future<SavedLocation?> reverseGeocode({
    required double latitude,
    required double longitude,
  }) async {
    state = state.copyWith(status: LocationStatus.reversGeocoding);
    try {
      final savedLocation = await _apiService.reverseGeocode(
        latitude: latitude,
        longitude: longitude,
      );
      state = state.copyWith(
        status: LocationStatus.success,
        location: savedLocation,
      );
      return savedLocation;
    } catch (e) {
      // API totally unreachable and all fallbacks exhausted — return null
      // so the UI keeps whatever address was shown before.
      state = state.copyWith(
        status: LocationStatus.error,
        errorMessage: e.toString(),
      );
      return null;
    }
  }

  /// Récupère les partenaires proches pour une position donnée
  Future<List<Map<String, dynamic>>> fetchNearbyPartners({
    required double latitude,
    required double longitude,
    int radiusMeters = 5000,
  }) async {
    try {
      final partners = await _apiService.getNearbyPartners(
        latitude: latitude,
        longitude: longitude,
        radiusMeters: radiusMeters,
      );
      return partners;
    } catch (e) {
      return [];
    }
  }

  void setLoading() => state = state.copyWith(status: LocationStatus.loading);

  void setDenied() => state = state.copyWith(status: LocationStatus.denied);

  void setServiceOff() =>
      state = state.copyWith(status: LocationStatus.serviceOff);

  void setError(String message) =>
      state = state.copyWith(
        status: LocationStatus.error,
        errorMessage: message,
      );

  void reset() => state = const LocationState();
}
