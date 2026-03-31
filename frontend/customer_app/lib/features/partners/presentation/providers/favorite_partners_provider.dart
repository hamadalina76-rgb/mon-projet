import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../config/dependency_injection/injection.dart';
import '../../data/datasources/partner_api_service.dart';
import '../../data/models/partner_nearby_dto.dart';
import 'nearby_partners_provider.dart';

enum FavoriteToggleAction { added, removed }

class FavoriteToggleResult {
  final FavoriteToggleAction action;
  final bool success;
  final String? errorMessage;

  const FavoriteToggleResult._({
    required this.action,
    required this.success,
    this.errorMessage,
  });

  factory FavoriteToggleResult.success(FavoriteToggleAction action) {
    return FavoriteToggleResult._(action: action, success: true);
  }

  factory FavoriteToggleResult.failure(
    FavoriteToggleAction action,
    String message,
  ) {
    return FavoriteToggleResult._(
      action: action,
      success: false,
      errorMessage: message,
    );
  }
}

class FavoritePartnersState {
  final Set<String> favoriteIds;
  final List<PartnerNearbyDto> favoritePartners;
  final bool isLoading;
  final bool isSyncing;
  final String? errorMessage;

  const FavoritePartnersState({
    this.favoriteIds = const <String>{},
    this.favoritePartners = const <PartnerNearbyDto>[],
    this.isLoading = false,
    this.isSyncing = false,
    this.errorMessage,
  });

  FavoritePartnersState copyWith({
    Set<String>? favoriteIds,
    List<PartnerNearbyDto>? favoritePartners,
    bool? isLoading,
    bool? isSyncing,
    String? errorMessage,
    bool clearError = false,
  }) {
    return FavoritePartnersState(
      favoriteIds: favoriteIds ?? this.favoriteIds,
      favoritePartners: favoritePartners ?? this.favoritePartners,
      isLoading: isLoading ?? this.isLoading,
      isSyncing: isSyncing ?? this.isSyncing,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }
}

class FavoritePartnersNotifier extends StateNotifier<FavoritePartnersState> {
  final Ref _ref;
  final PartnerApiService _apiService;

  FavoritePartnersNotifier(this._ref, this._apiService)
    : super(const FavoritePartnersState());

  String? _currentUserId() {
    final auth = _ref.read(authNotifierProvider);
    return auth.whenOrNull(authenticated: (u) => u.id);
  }

  bool isFavorite(String partnerId) => state.favoriteIds.contains(partnerId);

  Future<void> loadFavorites() async {
    if (state.isLoading) return;

    final userId = _currentUserId();
    if (userId == null) {
      state = const FavoritePartnersState();
      return;
    }

    state = state.copyWith(isLoading: true, clearError: true);

    try {
      final favorites = await _apiService.fetchFavorites(userId);
      final seen = <String>{};
      final orderedPartnerIds = <String>[];
      for (final entry in favorites) {
        if (seen.add(entry.partnerId)) {
          orderedPartnerIds.add(entry.partnerId);
        }
      }

      final seededById = {
        for (final partner in state.favoritePartners) partner.id: partner,
      };

      final resolvedPartners = await _resolveFavoritePartners(
        orderedPartnerIds,
        seededById: seededById,
      );

      state = state.copyWith(
        favoriteIds: orderedPartnerIds.toSet(),
        favoritePartners: resolvedPartners,
        isLoading: false,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Erreur, veuillez reessayer',
      );
    }
  }

  Future<void> refresh() async {
    await loadFavorites();
  }

  Future<FavoriteToggleResult> toggleFavorite({
    required String partnerId,
    PartnerNearbyDto? partnerSnapshot,
  }) async {
    final userId = _currentUserId();
    if (userId == null) {
      return FavoriteToggleResult.failure(
        FavoriteToggleAction.added,
        'Connectez-vous pour gerer vos favoris',
      );
    }

    final wasFavorite = state.favoriteIds.contains(partnerId);
    final action = wasFavorite
        ? FavoriteToggleAction.removed
        : FavoriteToggleAction.added;

    final previous = state;
    final nextIds = <String>{...state.favoriteIds};
    var nextPartners = List<PartnerNearbyDto>.from(state.favoritePartners);

    if (wasFavorite) {
      nextIds.remove(partnerId);
      nextPartners.removeWhere((p) => p.id == partnerId);
    } else {
      nextIds.add(partnerId);
      if (partnerSnapshot != null) {
        nextPartners = [
          partnerSnapshot,
          ...nextPartners.where((p) => p.id != partnerId),
        ];
      }
    }

    state = state.copyWith(
      favoriteIds: nextIds,
      favoritePartners: nextPartners,
      isSyncing: true,
      clearError: true,
    );

    try {
      if (wasFavorite) {
        await _apiService.removeFavorite(userId, partnerId);
      } else {
        await _apiService.addFavorite(userId, partnerId);
      }

      if (!wasFavorite && !nextPartners.any((p) => p.id == partnerId)) {
        try {
          final fullPartner = await _apiService.fetchPartnerById(partnerId);
          if (fullPartner.isActive) {
            nextPartners = [
              fullPartner,
              ...nextPartners.where((p) => p.id != partnerId),
            ];
          }
        } catch (_) {
          // Keep optimistic state; backend already accepted the favorite.
        }
      }

      state = state.copyWith(
        favoriteIds: nextIds,
        favoritePartners: nextPartners,
        isSyncing: false,
        clearError: true,
      );

      return FavoriteToggleResult.success(action);
    } catch (_) {
      state = previous.copyWith(
        isSyncing: false,
        errorMessage: 'Erreur, veuillez reessayer',
      );
      return FavoriteToggleResult.failure(action, 'Erreur, veuillez reessayer');
    }
  }

  Future<List<PartnerNearbyDto>> _resolveFavoritePartners(
    List<String> orderedPartnerIds, {
    Map<String, PartnerNearbyDto> seededById = const {},
  }) async {
    final futures = orderedPartnerIds.map((partnerId) async {
      final seeded = seededById[partnerId];
      if (seeded != null) return seeded;

      try {
        final partner = await _apiService.fetchPartnerById(partnerId);
        if (!partner.isActive) return null;
        return partner;
      } catch (_) {
        return null;
      }
    });

    final resolved = await Future.wait(futures);
    return resolved.whereType<PartnerNearbyDto>().toList();
  }
}

final favoritePartnersNotifierProvider =
    StateNotifierProvider<FavoritePartnersNotifier, FavoritePartnersState>(
      (ref) =>
          FavoritePartnersNotifier(ref, ref.read(partnerApiServiceProvider)),
    );
