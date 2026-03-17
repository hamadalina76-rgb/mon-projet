import 'package:flutter/material.dart';

class OpeningHourDto {
  final String dayOfWeek;
  final String? openTime;
  final String? closeTime;
  final bool isClosed;

  const OpeningHourDto({
    required this.dayOfWeek,
    this.openTime,
    this.closeTime,
    this.isClosed = false,
  });

  factory OpeningHourDto.fromJson(Map<String, dynamic> json) {
    final raw = json['dayOfWeek'];
    const dayNames = [
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];

    final dayOfWeek = (raw is int && raw >= 1 && raw <= 7)
        ? dayNames[raw - 1]
        : (raw?.toString() ?? '');

    return OpeningHourDto(
      dayOfWeek: dayOfWeek,
      openTime: json['openTime'] as String?,
      closeTime: json['closeTime'] as String?,
      isClosed: json['isClosed'] as bool? ?? false,
    );
  }
}

class PartnerNearbyDto {
  final String id;
  final String? businessName;
  final String? brandName;
  final String? slug;
  final String? type;
  final String? description;
  final String? shortDescription;
  final String? logo;
  final String? coverImage;
  final String? phoneNumber;
  final String? email;
  final String? address;
  final String? city;
  final double? latitude;
  final double? longitude;
  final int? deliveryRadius;
  final String? status;
  final bool isActive;
  final bool acceptsOrders;
  final bool isVerified;
  final bool isPremium;
  final bool isFeatured;
  final double? commissionRate;
  final int? preparationTime;
  final double? deliveryFee;
  final double? minimumOrder;
  final double? freeDeliveryThreshold;
  final double rating;
  final int totalRatings;
  final int? totalOrders;
  final List<OpeningHourDto> openingHours;
  final double? distanceKm;
  final DateTime? createdAt;
  final List<int> categoryIds;

  const PartnerNearbyDto({
    required this.id,
    this.businessName,
    this.brandName,
    this.slug,
    this.type,
    this.description,
    this.shortDescription,
    this.logo,
    this.coverImage,
    this.phoneNumber,
    this.email,
    this.address,
    this.city,
    this.latitude,
    this.longitude,
    this.deliveryRadius,
    this.status,
    this.isActive = true,
    this.acceptsOrders = true,
    this.isVerified = false,
    this.isPremium = false,
    this.isFeatured = false,
    this.commissionRate,
    this.preparationTime,
    this.deliveryFee,
    this.minimumOrder,
    this.freeDeliveryThreshold,
    this.rating = 0.0,
    this.totalRatings = 0,
    this.totalOrders,
    this.openingHours = const [],
    this.distanceKm,
    this.createdAt,
    this.categoryIds = const [],
  });

  factory PartnerNearbyDto.fromJson(Map<String, dynamic> json) {
    DateTime? created;
    final rawCreated = json['createdAt'];
    if (rawCreated != null) {
      try {
        created = DateTime.parse(rawCreated.toString());
      } catch (_) {}
    }

    final rawHours = json['openingHours'];
    final hours = (rawHours is List)
        ? rawHours
            .whereType<Map<String, dynamic>>()
            .map(OpeningHourDto.fromJson)
            .toList()
        : <OpeningHourDto>[];

    return PartnerNearbyDto(
      id: json['id']?.toString() ?? '',
      businessName: json['businessName'] as String?,
      brandName: json['brandName'] as String?,
      slug: json['slug'] as String?,
      type: json['type'] as String?,
      description: json['description'] as String?,
      shortDescription: json['shortDescription'] as String?,
      logo: json['logo'] as String?,
      coverImage: json['coverImage'] as String?,
      phoneNumber: json['phoneNumber'] as String?,
      email: json['email'] as String?,
      address: json['address'] as String?,
      city: json['city'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      deliveryRadius: (json['deliveryRadius'] as num?)?.toInt(),
      status: json['status'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      acceptsOrders: json['acceptsOrders'] as bool? ?? true,
      isVerified: json['isVerified'] as bool? ?? false,
      isPremium: json['isPremium'] as bool? ?? false,
      isFeatured: json['isFeatured'] as bool? ?? false,
      commissionRate: (json['commissionRate'] as num?)?.toDouble(),
      preparationTime: (json['preparationTime'] as num?)?.toInt(),
      deliveryFee: (json['deliveryFee'] as num?)?.toDouble(),
      minimumOrder: (json['minimumOrder'] as num?)?.toDouble(),
      freeDeliveryThreshold: (json['freeDeliveryThreshold'] as num?)?.toDouble(),
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
      totalRatings: (json['totalRatings'] as num?)?.toInt() ?? 0,
      totalOrders: (json['totalOrders'] as num?)?.toInt(),
      openingHours: hours,
      distanceKm: (json['distanceKm'] as num?)?.toDouble(),
      createdAt: created,
      categoryIds: (json['categoryIds'] is List)
          ? (json['categoryIds'] as List)
              .whereType<num>()
              .map((n) => n.toInt())
              .toList()
          : const [],
    );
  }

  String get displayName =>
      brandName?.isNotEmpty == true ? brandName! : (businessName ?? '');

  bool get isNew {
    if (createdAt == null) return false;
    return DateTime.now().difference(createdAt!).inDays < 30;
  }

  bool get isOpen {
    if (!isActive || !acceptsOrders) return false;
    if (openingHours.isEmpty) return isActive;

    final now = DateTime.now();
    const dayNames = [
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];
    final todayName = dayNames[now.weekday - 1];

    final todayHours =
        openingHours.where((h) => h.dayOfWeek.toUpperCase() == todayName).toList();

    if (todayHours.isEmpty) return true;
    final h = todayHours.first;
    if (h.isClosed) return false;
    if (h.openTime == null || h.closeTime == null) return true;

    try {
      final open = _parseTime(h.openTime!);
      final close = _parseTime(h.closeTime!);
      final currentMinutes = now.hour * 60 + now.minute;
      final openMinutes = open.hour * 60 + open.minute;
      final closeMinutes = close.hour * 60 + close.minute;
      return currentMinutes >= openMinutes && currentMinutes < closeMinutes;
    } catch (_) {
      return true;
    }
  }

  String? get todayCloseTime {
    if (openingHours.isEmpty) return null;
    const dayNames = [
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];
    final todayName = dayNames[DateTime.now().weekday - 1];
    final h = openingHours
        .where((h) => h.dayOfWeek.toUpperCase() == todayName)
        .firstWhere((_) => true, orElse: () => const OpeningHourDto(dayOfWeek: ''));
    return h.dayOfWeek.isEmpty ? null : h.closeTime;
  }

  ({bool opensToday, String time})? get nextOpenInfo {
    if (openingHours.isEmpty) return null;
    const dayNames = [
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];

    final now = DateTime.now();
    final nowMinutes = now.hour * 60 + now.minute;
    final todayName = dayNames[now.weekday - 1];

    OpeningHourDto? today;
    for (final h in openingHours) {
      if (h.dayOfWeek.toUpperCase() == todayName) {
        today = h;
        break;
      }
    }

    if (today != null && !today.isClosed && today.openTime != null) {
      try {
        final open = _parseTime(today.openTime!);
        final openMinutes = open.hour * 60 + open.minute;
        if (nowMinutes < openMinutes) {
          return (opensToday: true, time: today.openTime!);
        }
      } catch (_) {}
    }

    for (int d = 1; d <= 7; d++) {
      final nextDay = DateTime(now.year, now.month, now.day + d);
      final nextName = dayNames[nextDay.weekday - 1];
      for (final h in openingHours) {
        if (h.dayOfWeek.toUpperCase() == nextName && !h.isClosed && h.openTime != null) {
          return (opensToday: false, time: h.openTime!);
        }
      }
    }

    return null;
  }

  static TimeOfDay _parseTime(String time) {
    final parts = time.split(':');
    return TimeOfDay(hour: int.parse(parts[0]), minute: int.parse(parts[1]));
  }
}

class NearbyPartnersPage {
  final List<PartnerNearbyDto> content;
  final int pageNumber;
  final int pageSize;
  final int totalElements;

  const NearbyPartnersPage({
    required this.content,
    required this.pageNumber,
    required this.pageSize,
    required this.totalElements,
  });

  bool get isLastPage {
    final loaded = (pageNumber + 1) * pageSize;
    return loaded >= totalElements;
  }

  factory NearbyPartnersPage.fromJson(Map<String, dynamic> json) {
    final rawContent = json['content'];
    final content = (rawContent is List)
        ? rawContent
            .whereType<Map<String, dynamic>>()
            .map(PartnerNearbyDto.fromJson)
            .toList()
        : <PartnerNearbyDto>[];

    return NearbyPartnersPage(
      content: content,
      pageNumber: (json['pageNumber'] as num?)?.toInt() ?? 0,
      pageSize: (json['pageSize'] as num?)?.toInt() ?? 10,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? 0,
    );
  }
}
