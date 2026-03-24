import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/api/api_client.dart';
import '../../data/datasources/partner_api_service.dart';
import '../../data/models/category_dto.dart';
import '../../data/models/partner_nearby_dto.dart';

enum PartnerSortOption { popularity, rating, newest }

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
  final bool openNowOnly;
  final bool freeDeliveryOnly;
  final double? minRating;
  final int? maxDeliveryTime;
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
    this.sortOption = PartnerSortOption.popularity,
    this.selectedCategory = PartnerCategory.all,
    this.openNowOnly = false,
    this.freeDeliveryOnly = false,
    this.minRating,
    this.maxDeliveryTime,
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
    bool? openNowOnly,
    bool? freeDeliveryOnly,
    double? minRating,
    bool clearMinRating = false,
    int? maxDeliveryTime,
    bool clearMaxDeliveryTime = false,
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
        openNowOnly: openNowOnly ?? this.openNowOnly,
        freeDeliveryOnly: freeDeliveryOnly ?? this.freeDeliveryOnly,
        minRating: clearMinRating ? null : (minRating ?? this.minRating),
        maxDeliveryTime: clearMaxDeliveryTime
          ? null
          : (maxDeliveryTime ?? this.maxDeliveryTime),
      selectedCategoryId: clearCategoryId
          ? null
          : (selectedCategoryId ?? this.selectedCategoryId),
      selectedSubCategoryIds: clearSubCategoryIds
          ? <int>{}
          : (selectedSubCategoryIds ?? this.selectedSubCategoryIds),
    );
  }

  List<PartnerNearbyDto> get filteredPartners {
    return _applyFilters(allPartners);
  }

  int get activeFilterCount {
    var count = 0;
    if (openNowOnly) count++;
    if (freeDeliveryOnly) count++;
    if (minRating != null) count++;
    if (maxDeliveryTime != null) count++;
    return count;
  }

  List<PartnerNearbyDto> previewFilteredPartners({
    bool? openNowOnly,
    bool? freeDeliveryOnly,
    double? minRating,
    int? maxDeliveryTime,
    PartnerSortOption? sortOption,
  }) {
    return _applyFilters(
      allPartners,
      openNowOnly: openNowOnly,
      freeDeliveryOnly: freeDeliveryOnly,
      minRating: minRating,
      maxDeliveryTime: maxDeliveryTime,
      sortOption: sortOption,
    );
  }

  List<PartnerNearbyDto> _applyFilters(
    List<PartnerNearbyDto> source, {
    bool? openNowOnly,
    bool? freeDeliveryOnly,
    double? minRating,
    int? maxDeliveryTime,
    PartnerSortOption? sortOption,
  }) {
    var list = source;

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

    final effectiveOpenNowOnly = openNowOnly ?? this.openNowOnly;
    final effectiveFreeDeliveryOnly =
        freeDeliveryOnly ?? this.freeDeliveryOnly;
    final effectiveMinRating = minRating ?? this.minRating;
    final effectiveMaxDeliveryTime = maxDeliveryTime ?? this.maxDeliveryTime;

    if (effectiveOpenNowOnly) {
      list = list.where((p) => p.isOpen).toList();
    }

    if (effectiveFreeDeliveryOnly) {
      list = list.where((p) => (p.deliveryFee ?? 999999) == 0).toList();
    }

    if (effectiveMinRating != null) {
      list = list.where((p) => p.rating >= effectiveMinRating).toList();
    }

    if (effectiveMaxDeliveryTime != null) {
      list = list
          .where(
            (p) =>
                p.preparationTime != null &&
                p.preparationTime! <= effectiveMaxDeliveryTime,
          )
          .toList();
    }

    return _applySorting(list, sortOption: sortOption ?? this.sortOption);
  }

  List<PartnerNearbyDto> _applySorting(
    List<PartnerNearbyDto> list, {
    required PartnerSortOption sortOption,
  }) {
    final sorted = List<PartnerNearbyDto>.from(list);
    sorted.sort((a, b) {
      if (a.isOpen != b.isOpen) {
        return a.isOpen ? -1 : 1;
      }

      final da = a.distanceKm ?? double.infinity;
      final db = b.distanceKm ?? double.infinity;
      final byDistance = da.compareTo(db);
      if (byDistance != 0) return byDistance;

      final byRating = b.rating.compareTo(a.rating);
      if (byRating != 0) return byRating;

      switch (sortOption) {
        case PartnerSortOption.popularity:
          if (a.isFeatured != b.isFeatured) {
            return a.isFeatured ? -1 : 1;
          }
          final ao = a.totalOrders ?? 0;
          final bo = b.totalOrders ?? 0;
          return bo.compareTo(ao);
        case PartnerSortOption.rating:
          return byRating;
        case PartnerSortOption.newest:
          final ad = a.createdAt;
          final bd = b.createdAt;
          if (ad == null && bd == null) return 0;
          if (ad == null) return 1;
          if (bd == null) return -1;
          return bd.compareTo(ad);
      }
    });

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

  void setOpenNowOnly(bool value) {
    state = state.copyWith(openNowOnly: value);
  }

  void setFreeDeliveryOnly(bool value) {
    state = state.copyWith(freeDeliveryOnly: value);
  }

  void setMinRating(double? value) {
    state = state.copyWith(minRating: value, clearMinRating: value == null);
  }

  void setMaxDeliveryTime(int? value) {
    state = state.copyWith(
      maxDeliveryTime: value,
      clearMaxDeliveryTime: value == null,
    );
  }

  void resetFilters() {
    state = state.copyWith(
      openNowOnly: false,
      freeDeliveryOnly: false,
      clearMinRating: true,
      clearMaxDeliveryTime: true,
      sortOption: PartnerSortOption.popularity,
    );
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
