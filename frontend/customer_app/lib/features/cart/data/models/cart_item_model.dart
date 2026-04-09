class CartItemSelectedOption {
  final String? optionId;
  final String optionName;
  final String? valueId;
  final String valueName;
  final double priceModifier;

  const CartItemSelectedOption({
    this.optionId,
    required this.optionName,
    this.valueId,
    required this.valueName,
    this.priceModifier = 0,
  });

  factory CartItemSelectedOption.freeText(String label) {
    final normalized = label.trim();
    return CartItemSelectedOption(
      optionName: '',
      valueName: normalized,
      priceModifier: 0,
    );
  }

  String get displayLabel {
    final group = optionName.trim();
    final value = valueName.trim();

    if (group.isEmpty && value.isEmpty) return '';
    if (group.isEmpty) return value;
    if (value.isEmpty) return group;
    if (group.toLowerCase() == value.toLowerCase()) return value;
    return '$group: $value';
  }

  Map<String, dynamic> toPayloadJson() {
    final map = <String, dynamic>{
      'optionName': optionName,
      'valueName': valueName,
      'priceModifier': priceModifier,
    };

    if (optionId != null && optionId!.trim().isNotEmpty) {
      map['optionId'] = optionId!.trim();
    }

    if (valueId != null && valueId!.trim().isNotEmpty) {
      map['valueId'] = valueId!.trim();
    }

    return map;
  }

  static CartItemSelectedOption? fromDynamic(dynamic raw) {
    if (raw is String) {
      final trimmed = raw.trim();
      if (trimmed.isEmpty) return null;

      if (trimmed.contains(':')) {
        final parts = trimmed.split(':');
        final group = parts.first.trim();
        final value = parts.sublist(1).join(':').trim();
        return CartItemSelectedOption(
          optionName: group,
          valueName: value.isEmpty ? group : value,
          priceModifier: 0,
        );
      }

      return CartItemSelectedOption.freeText(trimmed);
    }

    if (raw is! Map) return null;

    final map = Map<String, dynamic>.from(raw);
    final optionName = (map['optionName'] ?? map['groupName'] ?? '')
        .toString()
        .trim();
    final valueName =
        (map['valueName'] ?? map['name'] ?? map['label'] ?? optionName)
            .toString()
            .trim();
    final optionId = map['optionId']?.toString().trim();
    final valueId = map['valueId']?.toString().trim();
    final priceRaw = map['priceModifier'] ?? map['price'] ?? 0;
    final priceModifier = priceRaw is num
      ? priceRaw.toDouble()
      : double.tryParse(priceRaw.toString()) ?? 0;

    if (optionName.isEmpty && valueName.isEmpty) {
      return null;
    }

    return CartItemSelectedOption(
      optionId: optionId?.isEmpty == true ? null : optionId,
      optionName: optionName,
      valueId: valueId?.isEmpty == true ? null : valueId,
      valueName: valueName,
      priceModifier: priceModifier,
    );
  }

  static List<CartItemSelectedOption> fromDynamicList(dynamic raw) {
    if (raw is! List) return const <CartItemSelectedOption>[];

    return raw
        .map(fromDynamic)
        .whereType<CartItemSelectedOption>()
        .toList();
  }
}

class CartItemModel {
  final String productId;

  final String partnerId;

  final String partnerName;

  final String partnerLogoUrl;

  final String productName;

  final double unitPrice;

  int quantity;

  final List<CartItemSelectedOption> selectedOptions;

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

  List<String> get selectedOptionsDisplay =>
      selectedOptions.map((option) => option.displayLabel).toList();

  List<Map<String, dynamic>> get selectedOptionsPayload =>
      selectedOptions.map((option) => option.toPayloadJson()).toList();

  /// Key used to merge same product + same customization lines.
  String get uniqueKey {
    final optionsSignature =
        selectedOptions
            .map(
              (e) =>
                  '${e.displayLabel.trim().toLowerCase()}@${e.priceModifier.toStringAsFixed(2)}',
            )
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
    List<CartItemSelectedOption>? selectedOptions,
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
      selectedOptions: selectedOptions ??
          List<CartItemSelectedOption>.from(this.selectedOptions),
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
      'selectedOptions': selectedOptionsPayload,
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
      selectedOptions: CartItemSelectedOption.fromDynamicList(
        json['selectedOptions'] ?? json['selectedOptionLabels'],
      ),
      kitchenNote: json['kitchenNote']?.toString(),
    );
  }
}
