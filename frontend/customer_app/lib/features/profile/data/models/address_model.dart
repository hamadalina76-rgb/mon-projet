import '../../../../features/location/data/models/saved_location.dart';

/// Modèle Flutter pour une adresse de livraison (mappé sur AddressDTO backend)
class AddressModel {
  final int id;
  final int customerId;
  final AddressType type;
  final String? label;
  final String? street;
  final String? building;
  final String? floor;
  final String? apartment;
  final String? city;
  final String? postalCode;
  final String? state;
  final String? country;
  final double? latitude;
  final double? longitude;
  final String? formattedAddress;
  final String? deliveryInstructions;
  final bool isDefault;
  final bool isVerified;

  const AddressModel({
    required this.id,
    required this.customerId,
    required this.type,
    this.label,
    this.street,
    this.building,
    this.floor,
    this.apartment,
    this.city,
    this.postalCode,
    this.state,
    this.country,
    this.latitude,
    this.longitude,
    this.formattedAddress,
    this.deliveryInstructions,
    this.isDefault = false,
    this.isVerified = false,
  });

  factory AddressModel.fromJson(Map<String, dynamic> json) {
    return AddressModel(
      id: json['id'] as int,
      customerId: json['customerId'] as int,
      type: AddressTypeX.fromString(
          (json['type'] as String?)?.toLowerCase()),
      label: json['label'] as String?,
      street: json['street'] as String?,
      building: json['building'] as String?,
      floor: json['floor'] as String?,
      apartment: json['apartment'] as String?,
      city: json['city'] as String?,
      postalCode: json['postalCode'] as String?,
      state: json['state'] as String?,
      country: json['country'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      formattedAddress: json['formattedAddress'] as String?,
      deliveryInstructions: json['deliveryInstructions'] as String?,
      isDefault: json['isDefault'] as bool? ?? false,
      isVerified: json['isVerified'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'customerId': customerId,
        'type': type.value.toUpperCase(),
        if (label != null) 'label': label,
        if (street != null) 'street': street,
        if (building != null) 'building': building,
        if (floor != null) 'floor': floor,
        if (apartment != null) 'apartment': apartment,
        if (city != null) 'city': city,
        if (postalCode != null) 'postalCode': postalCode,
        if (state != null) 'state': state,
        if (country != null) 'country': country,
        if (latitude != null) 'latitude': latitude,
        if (longitude != null) 'longitude': longitude,
        if (formattedAddress != null) 'formattedAddress': formattedAddress,
        if (deliveryInstructions != null)
          'deliveryInstructions': deliveryInstructions,
        'isDefault': isDefault,
        'isVerified': isVerified,
      };

  /// Affichage: label personnalisé ou libellé par défaut du type
  String get displayLabel {
    if (label != null && label!.isNotEmpty) return label!;
    switch (type) {
      case AddressType.home:      return 'Maison';
      case AddressType.work:      return 'Bureau';
      case AddressType.apartment: return 'Appartement';
      case AddressType.other:     return 'Autre';
    }
  }

  AddressModel copyWith({
    int? id,
    int? customerId,
    AddressType? type,
    String? label,
    String? street,
    String? building,
    String? floor,
    String? apartment,
    String? city,
    String? postalCode,
    String? state,
    String? country,
    double? latitude,
    double? longitude,
    String? formattedAddress,
    String? deliveryInstructions,
    bool? isDefault,
    bool? isVerified,
  }) =>
      AddressModel(
        id: id ?? this.id,
        customerId: customerId ?? this.customerId,
        type: type ?? this.type,
        label: label ?? this.label,
        street: street ?? this.street,
        building: building ?? this.building,
        floor: floor ?? this.floor,
        apartment: apartment ?? this.apartment,
        city: city ?? this.city,
        postalCode: postalCode ?? this.postalCode,
        state: state ?? this.state,
        country: country ?? this.country,
        latitude: latitude ?? this.latitude,
        longitude: longitude ?? this.longitude,
        formattedAddress: formattedAddress ?? this.formattedAddress,
        deliveryInstructions: deliveryInstructions ?? this.deliveryInstructions,
        isDefault: isDefault ?? this.isDefault,
        isVerified: isVerified ?? this.isVerified,
      );
}

/// DTO pour créer / mettre à jour une adresse
class AddressRequest {
  final AddressType type;
  final String? label;
  final String? street;
  final String? building;
  final String? floor;
  final String? apartment;
  final String? city;
  final String? postalCode;
  final String? state;
  final String? country;
  final double? latitude;
  final double? longitude;
  final String? formattedAddress;
  final String? deliveryInstructions;
  final bool? isDefault;

  const AddressRequest({
    required this.type,
    this.label,
    this.street,
    this.building,
    this.floor,
    this.apartment,
    this.city,
    this.postalCode,
    this.state,
    this.country,
    this.latitude,
    this.longitude,
    this.formattedAddress,
    this.deliveryInstructions,
    this.isDefault,
  });

  Map<String, dynamic> toJson() => {
        'type': type.value.toUpperCase(),
        if (label != null) 'label': label,
        if (street != null) 'street': street,
        if (building != null) 'building': building,
        if (floor != null) 'floor': floor,
        if (apartment != null) 'apartment': apartment,
        if (city != null) 'city': city,
        if (postalCode != null) 'postalCode': postalCode,
        if (state != null) 'state': state,
        if (country != null) 'country': country,
        if (latitude != null) 'latitude': latitude,
        if (longitude != null) 'longitude': longitude,
        if (formattedAddress != null) 'formattedAddress': formattedAddress,
        if (deliveryInstructions != null)
          'deliveryInstructions': deliveryInstructions,
        if (isDefault != null) 'isDefault': isDefault,
      };
}
