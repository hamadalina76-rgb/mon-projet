import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

class PartnerHeader extends StatelessWidget {
  final String partnerName;
  final String partnerLogoUrl;

  const PartnerHeader({
    super.key,
    required this.partnerName,
    required this.partnerLogoUrl,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.black12),
      ),
      child: Row(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(22),
            child: partnerLogoUrl.trim().isEmpty
                ? Container(
                    width: 44,
                    height: 44,
                    color: const Color(0xFFF3F3F3),
                    child: const Icon(Icons.storefront),
                  )
                : CachedNetworkImage(
                    imageUrl: partnerLogoUrl,
                    width: 44,
                    height: 44,
                    fit: BoxFit.cover,
                    errorWidget: (_, __, ___) => Container(
                      width: 44,
                      height: 44,
                      color: const Color(0xFFF3F3F3),
                      child: const Icon(Icons.storefront),
                    ),
                  ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              partnerName,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }
}
