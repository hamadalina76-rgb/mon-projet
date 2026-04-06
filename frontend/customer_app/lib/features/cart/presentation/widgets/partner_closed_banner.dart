import 'package:flutter/material.dart';

class PartnerClosedBanner extends StatelessWidget {
  const PartnerClosedBanner({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 48,
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.symmetric(horizontal: 12),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: const Color(0xFFFFF2E4),
        borderRadius: BorderRadius.circular(12),
      ),
      child: const Text(
        'Le partenaire est actuellement fermé. Vous pouvez préparer votre panier.',
        textAlign: TextAlign.center,
        style: TextStyle(color: Color(0xFFB36A18), fontWeight: FontWeight.w700),
      ),
    );
  }
}
