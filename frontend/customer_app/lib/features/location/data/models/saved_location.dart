import 'dart:convert';

/// Type d'adresse
enum AddressType { home, work, other }

extension AddressTypeX on AddressType {
  String get key {
    switch (this) {
      case AddressType.home: return 'home_address';
      case AddressType.work: return 'work_address';
      case AddressType.other: return 'other_address';
    }
  }

  static AddressType fromString(String? v) {
    switch (v) {
      case 'work': return AddressType.work;
      case 'other': return AddressType.other;
      default: return AddressType.home;
    }
  }

  String get value {
    switch (this) {
      case AddressType.home: return 'home';
      case AddressType.work: return 'work';
      case AddressType.other: return 'other';
    }
  }
}

/// Modèle pour une localisation sauvegardée
/// Stocké dans Hive sous la clé 'current_location'
class SavedLocation {
  final double latitude;
  final double longitude;
  final String formattedAddress;
  final String street;
  final String city;
  final String state;
  final String postalCode;
  final String country;
  final DateTime savedAt;
  final AddressType addressType;
  final String? customLabel;

  const SavedLocation({
    required this.latitude,
    required this.longitude,
    required this.formattedAddress,
    this.street = '',
    this.city = '',
    this.state = '',
    this.postalCode = '',
    this.country = 'Tunisie',
    required this.savedAt,
    this.addressType = AddressType.home,
    this.customLabel,
  });

  /// Affichage court : "Rue, Ville"
  String get shortAddress {
    if (street.isNotEmpty && city.isNotEmpty) {
      return '$street, $city';
    } else if (city.isNotEmpty) {
      return city;
    }
    return formattedAddress;
  }

  /// Coordonnées GPS
  String get coordinates => '${latitude.toStringAsFixed(6)}, ${longitude.toStringAsFixed(6)}';

  // === Sérialisation JSON pour Hive ===

  Map<String, dynamic> toMap() {
    return {
      'latitude': latitude,
      'longitude': longitude,
      'formattedAddress': formattedAddress,
      'street': street,
      'city': city,
      'state': state,
      'postalCode': postalCode,
      'country': country,
      'savedAt': savedAt.toIso8601String(),
      'addressType': addressType.value,
      'customLabel': customLabel,
    };
  }

  String toJson() => jsonEncode(toMap());

  factory SavedLocation.fromMap(Map<dynamic, dynamic> map) {
    return SavedLocation(
      latitude: (map['latitude'] as num).toDouble(),
      longitude: (map['longitude'] as num).toDouble(),
      formattedAddress: map['formattedAddress'] as String? ?? '',
      street: map['street'] as String? ?? '',
      city: map['city'] as String? ?? '',
      state: map['state'] as String? ?? '',
      postalCode: map['postalCode'] as String? ?? '',
      country: map['country'] as String? ?? 'Tunisie',
      savedAt: DateTime.tryParse(map['savedAt'] as String? ?? '') ?? DateTime.now(),
      addressType: AddressTypeX.fromString(map['addressType'] as String?),
      customLabel: map['customLabel'] as String?,
    );
  }

  factory SavedLocation.fromJson(String json) {
    return SavedLocation.fromMap(jsonDecode(json) as Map<dynamic, dynamic>);
  }

  SavedLocation copyWith({
    double? latitude,
    double? longitude,
    String? formattedAddress,
    String? street,
    String? city,
    String? state,
    String? postalCode,
    String? country,
    DateTime? savedAt,
    AddressType? addressType,
    String? customLabel,
  }) {
    return SavedLocation(
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      formattedAddress: formattedAddress ?? this.formattedAddress,
      street: street ?? this.street,
      city: city ?? this.city,
      state: state ?? this.state,
      postalCode: postalCode ?? this.postalCode,
      country: country ?? this.country,
      savedAt: savedAt ?? this.savedAt,
      addressType: addressType ?? this.addressType,
      customLabel: customLabel ?? this.customLabel,
    );
  }

  @override
  String toString() => 'SavedLocation(lat=$latitude, lon=$longitude, address=$formattedAddress)';
}
