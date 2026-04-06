class CartItemModel {
  final String productId;

  final String partnerId;

  final String partnerName;

  final String partnerLogoUrl;

  final String productName;

  final double unitPrice;

  int quantity;

  final List<String> selectedOptions;

  final String? kitchenNote;

  CartItemModel({
    required this.productId,
    required this.partnerId,
    required this.partnerName,
    required this.partnerLogoUrl,
    required this.productName,
    required this.unitPrice,
    required this.quantity,
    required this.selectedOptions,
    this.kitchenNote,
  });

  double get lineTotal => unitPrice * quantity;

  /// Key used to merge same product + same customization lines.
  String get uniqueKey {
    final optionsSignature =
        selectedOptions
            .map((e) => e.trim().toLowerCase())
            .where((e) => e.isNotEmpty)
            .toList()
          ..sort();
    final note = (kitchenNote ?? '').trim().toLowerCase();
    return '$productId|${optionsSignature.join(',')}|$note';
  }

  CartItemModel copyWith({
    String? productId,
    String? partnerId,
    String? partnerName,
    String? partnerLogoUrl,
    String? productName,
    double? unitPrice,
    int? quantity,
    List<String>? selectedOptions,
    String? kitchenNote,
    bool clearKitchenNote = false,
  }) {
    return CartItemModel(
      productId: productId ?? this.productId,
      partnerId: partnerId ?? this.partnerId,
      partnerName: partnerName ?? this.partnerName,
      partnerLogoUrl: partnerLogoUrl ?? this.partnerLogoUrl,
      productName: productName ?? this.productName,
      unitPrice: unitPrice ?? this.unitPrice,
      quantity: quantity ?? this.quantity,
      selectedOptions:
          selectedOptions ?? List<String>.from(this.selectedOptions),
      kitchenNote: clearKitchenNote ? null : (kitchenNote ?? this.kitchenNote),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productId': productId,
      'partnerId': partnerId,
      'partnerName': partnerName,
      'partnerLogoUrl': partnerLogoUrl,
      'productName': productName,
      'unitPrice': unitPrice,
      'quantity': quantity,
      'selectedOptions': selectedOptions,
      'kitchenNote': kitchenNote,
    };
  }

  factory CartItemModel.fromJson(Map<String, dynamic> json) {
    return CartItemModel(
      productId: json['productId']?.toString() ?? '',
      partnerId: json['partnerId']?.toString() ?? '',
      partnerName: json['partnerName']?.toString() ?? '',
      partnerLogoUrl: json['partnerLogoUrl']?.toString() ?? '',
      productName: json['productName']?.toString() ?? '',
      unitPrice: (json['unitPrice'] as num?)?.toDouble() ?? 0,
      quantity: (json['quantity'] as num?)?.toInt() ?? 1,
      selectedOptions: (json['selectedOptions'] is List)
          ? (json['selectedOptions'] as List).map((e) => e.toString()).toList()
          : const <String>[],
      kitchenNote: json['kitchenNote']?.toString(),
    );
  }
}
