import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/api/api_client.dart';
import '../../data/datasources/partner_api_service.dart';
import '../../data/models/category_dto.dart';
import '../../data/models/partner_nearby_dto.dart';

enum PartnerSortOption { recommended, nearMe, bestRated, deliveryFee }

enum PartnerCategory { all, restaurant, grocery, courier, pharmacy, other }

class NearbyPartnersState {
  final List<PartnerNearbyDto> allPartners;
  final bool isLoading;
  final bool isLoadingMore;
  final bool isRefreshing;
  final String? errorMessage;
  final int currentPage;
  final bool hasReachedEnd;
  final PartnerSortOption sortOption;
  final PartnerCategory selectedCategory;
  final bool promotionsOnly;
  final int? selectedCategoryId;
  final Set<int> selectedSubCategoryIds;

  const NearbyPartnersState({
    this.allPartners = const [],
    this.isLoading = false,
    this.isLoadingMore = false,
    this.isRefreshing = false,
    this.errorMessage,
    this.currentPage = 0,
    this.hasReachedEnd = false,
    this.sortOption = PartnerSortOption.recommended,
    this.selectedCategory = PartnerCategory.all,
    this.promotionsOnly = false,
    this.selectedCategoryId,
    this.selectedSubCategoryIds = const <int>{},
  });

  NearbyPartnersState copyWith({
    List<PartnerNearbyDto>? allPartners,
    bool? isLoading,
    bool? isLoadingMore,
    bool? isRefreshing,
    String? errorMessage,
    bool clearError = false,
    int? currentPage,
    bool? hasReachedEnd,
    PartnerSortOption? sortOption,
    PartnerCategory? selectedCategory,
    bool? promotionsOnly,
    int? selectedCategoryId,
    bool clearCategoryId = false,
    Set<int>? selectedSubCategoryIds,
    bool clearSubCategoryIds = false,
  }) {
    return NearbyPartnersState(
      allPartners: allPartners ?? this.allPartners,
      isLoading: isLoading ?? this.isLoading,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      isRefreshing: isRefreshing ?? this.isRefreshing,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      currentPage: currentPage ?? this.currentPage,
      hasReachedEnd: hasReachedEnd ?? this.hasReachedEnd,
      sortOption: sortOption ?? this.sortOption,
      selectedCategory: selectedCategory ?? this.selectedCategory,
      promotionsOnly: promotionsOnly ?? this.promotionsOnly,
      selectedCategoryId: clearCategoryId
          ? null
          : (selectedCategoryId ?? this.selectedCategoryId),
      selectedSubCategoryIds: clearSubCategoryIds
          ? <int>{}
          : (selectedSubCategoryIds ?? this.selectedSubCategoryIds),
    );
  }

  List<PartnerNearbyDto> get filteredPartners {
    var list = allPartners;

    if (selectedSubCategoryIds.isNotEmpty) {
      list = list
          .where((p) => p.categoryIds.any(selectedSubCategoryIds.contains))
          .toList();
    } else if (selectedCategoryId != null) {
      list = list.where((p) => p.categoryIds.contains(selectedCategoryId)).toList();
    } else if (selectedCategory != PartnerCategory.all) {
      list = list.where((p) {
        final t = p.type?.toUpperCase() ?? '';
        switch (selectedCategory) {
          case PartnerCategory.restaurant:
            return t.contains('RESTAURANT') || t.contains('FOOD');
          case PartnerCategory.grocery:
            return t.contains('GROCERY') ||
                t.contains('SUPERMARKET') ||
                t.contains('EPICERIE');
          case PartnerCategory.courier:
            return t.contains('COURIER') || t.contains('EXPRESS');
          case PartnerCategory.pharmacy:
            return t.contains('PHARMACY') || t.contains('PHARMACIE');
          case PartnerCategory.other:
            return !t.contains('RESTAURANT') &&
                !t.contains('FOOD') &&
                !t.contains('GROCERY') &&
                !t.contains('COURIER') &&
                !t.contains('PHARMACY');
          case PartnerCategory.all:
            return true;
        }
      }).toList();
    }

    if (promotionsOnly) {
      list = list
          .where((p) =>
              p.freeDeliveryThreshold != null ||
              (p.deliveryFee != null && p.deliveryFee! == 0))
          .toList();
    }

    list = _applySorting(list);

    final open = list.where((p) => p.isOpen).toList();
    final closed = list.where((p) => !p.isOpen).toList();
    return [...open, ...closed];
  }

  List<PartnerNearbyDto> _applySorting(List<PartnerNearbyDto> list) {
    final sorted = List<PartnerNearbyDto>.from(list);
    switch (sortOption) {
      case PartnerSortOption.recommended:
        sorted.sort((a, b) {
          if (a.isFeatured != b.isFeatured) {
            return a.isFeatured ? -1 : 1;
          }
          return b.rating.compareTo(a.rating);
        });
      case PartnerSortOption.nearMe:
        sorted.sort((a, b) {
          final da = a.distanceKm ?? double.infinity;
          final db = b.distanceKm ?? double.infinity;
          return da.compareTo(db);
        });
      case PartnerSortOption.bestRated:
        sorted.sort((a, b) => b.rating.compareTo(a.rating));
      case PartnerSortOption.deliveryFee:
        sorted.sort((a, b) {
          final da = a.deliveryFee ?? double.infinity;
          final db = b.deliveryFee ?? double.infinity;
          return da.compareTo(db);
        });
    }
    return sorted;
  }

  List<PartnerNearbyDto> get popularPartners {
    final open = allPartners.where((p) => p.isOpen).toList()
      ..sort((a, b) => b.rating.compareTo(a.rating));
    return open.take(5).toList();
  }

  Map<PartnerCategory, int> get categoryCounts {
    return {
      PartnerCategory.all: allPartners.length,
      PartnerCategory.restaurant: allPartners.where((p) {
        final t = p.type?.toUpperCase() ?? '';
        return t.contains('RESTAURANT') || t.contains('FOOD');
      }).length,
      PartnerCategory.grocery: allPartners.where((p) {
        final t = p.type?.toUpperCase() ?? '';
        return t.contains('GROCERY') ||
            t.contains('SUPERMARKET') ||
            t.contains('EPICERIE');
      }).length,
      PartnerCategory.courier: allPartners.where((p) {
        final t = p.type?.toUpperCase() ?? '';
        return t.contains('COURIER') || t.contains('EXPRESS');
      }).length,
      PartnerCategory.pharmacy: allPartners.where((p) {
        final t = p.type?.toUpperCase() ?? '';
        return t.contains('PHARMACY') || t.contains('PHARMACIE');
      }).length,
    };
  }
}

class NearbyPartnersNotifier extends StateNotifier<NearbyPartnersState> {
  final PartnerApiService _apiService;

  static const int _pageSize = 10;

  NearbyPartnersNotifier(this._apiService) : super(const NearbyPartnersState());

  Future<void> load({required double lat, required double lng}) async {
    if (state.isLoading) return;
    state = state.copyWith(
      isLoading: true,
      clearError: true,
      currentPage: 0,
      hasReachedEnd: false,
    );
    try {
      final page = await _apiService.fetchNearbyPartners(
        lat: lat,
        lng: lng,
        page: 0,
        size: _pageSize,
      );
      state = state.copyWith(
        isLoading: false,
        allPartners: page.content,
        currentPage: 0,
        hasReachedEnd: page.isLastPage,
      );
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.toString());
    }
  }

  Future<void> refresh({required double lat, required double lng}) async {
    if (state.isRefreshing) return;
    state = state.copyWith(isRefreshing: true, clearError: true);
    try {
      final page = await _apiService.fetchNearbyPartners(
        lat: lat,
        lng: lng,
        page: 0,
        size: _pageSize,
      );
      state = state.copyWith(
        isRefreshing: false,
        allPartners: page.content,
        currentPage: 0,
        hasReachedEnd: page.isLastPage,
      );
    } catch (e) {
      state = state.copyWith(isRefreshing: false, errorMessage: e.toString());
    }
  }

  Future<void> loadMore({required double lat, required double lng}) async {
    if (state.isLoadingMore || state.hasReachedEnd || state.isLoading) return;
    state = state.copyWith(isLoadingMore: true);
    try {
      final nextPage = state.currentPage + 1;
      final page = await _apiService.fetchNearbyPartners(
        lat: lat,
        lng: lng,
        page: nextPage,
        size: _pageSize,
      );
      state = state.copyWith(
        isLoadingMore: false,
        allPartners: [...state.allPartners, ...page.content],
        currentPage: nextPage,
        hasReachedEnd: page.isLastPage,
      );
    } catch (e) {
      state = state.copyWith(isLoadingMore: false, errorMessage: e.toString());
    }
  }

  void setSortOption(PartnerSortOption option) {
    state = state.copyWith(sortOption: option);
  }

  void setCategory(PartnerCategory category) {
    state = state.copyWith(
      selectedCategory: category,
      clearCategoryId: true,
      clearSubCategoryIds: true,
    );
  }

  void setCategoryById(int? id) {
    state = state.copyWith(
      selectedCategoryId: id,
      clearCategoryId: id == null,
      clearSubCategoryIds: true,
      selectedCategory: PartnerCategory.all,
    );
  }

  void toggleSubCategory(int id) {
    final next = <int>{...state.selectedSubCategoryIds};
    if (next.contains(id)) {
      next.remove(id);
    } else {
      next.add(id);
    }
    state = state.copyWith(
      selectedSubCategoryIds: next,
      selectedCategory: PartnerCategory.all,
    );
  }

  void togglePromotions() {
    state = state.copyWith(promotionsOnly: !state.promotionsOnly);
  }

  void clearError() {
    state = state.copyWith(clearError: true);
  }
}

final categoriesProvider = FutureProvider<List<CategoryDto>>((ref) {
  return ref.read(partnerApiServiceProvider).fetchCategories();
});

final partnerApiServiceProvider = Provider<PartnerApiService>(
  (ref) => PartnerApiService(dio: ApiClient().dio),
);

final nearbyPartnersNotifierProvider =
    StateNotifierProvider<NearbyPartnersNotifier, NearbyPartnersState>(
  (ref) => NearbyPartnersNotifier(ref.watch(partnerApiServiceProvider)),
);
