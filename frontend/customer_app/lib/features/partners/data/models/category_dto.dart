class CategoryDto {
  final int id;
  final Map<String, String> nameI18n;
  final String? slug;
  final String? icon;
  final String? image;
  final int? parentId;
  final int displayOrder;
  final bool isActive;
  final bool isFeatured;
  final String? backgroundColor;
  final String? textColor;
  final String? categoryBusinessType;

  const CategoryDto({
    required this.id,
    required this.nameI18n,
    this.slug,
    this.icon,
    this.image,
    this.parentId,
    this.displayOrder = 0,
    this.isActive = true,
    this.isFeatured = false,
    this.backgroundColor,
    this.textColor,
    this.categoryBusinessType,
  });

  factory CategoryDto.fromJson(Map<String, dynamic> json) {
    final rawI18n = json['nameI18n'];
    final Map<String, String> names = rawI18n is Map
        ? {
            for (final e in rawI18n.entries)
              e.key.toString(): e.value?.toString() ?? '',
          }
        : {};

    return CategoryDto(
      id: (json['id'] as num).toInt(),
      nameI18n: names,
      slug: json['slug'] as String?,
      icon: json['icon'] as String?,
      image: json['image'] as String?,
      parentId: (json['parentId'] as num?)?.toInt(),
      displayOrder: (json['displayOrder'] as num?)?.toInt() ?? 0,
      isActive: json['isActive'] as bool? ?? true,
      isFeatured: json['isFeatured'] as bool? ?? false,
      backgroundColor: json['backgroundColor'] as String?,
      textColor: json['textColor'] as String?,
      categoryBusinessType: json['categoryBusinessType'] as String?,
    );
  }

  String localizedName(String locale) {
    return nameI18n[locale] ??
        nameI18n['fr'] ??
        nameI18n['en'] ??
        (nameI18n.isNotEmpty ? nameI18n.values.first : null) ??
        slug ??
        'Category $id';
  }
}
