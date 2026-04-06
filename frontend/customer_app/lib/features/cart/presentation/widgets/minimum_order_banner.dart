import 'package:flutter/material.dart';

class MinimumOrderBanner extends StatelessWidget {
  final double minimumOrder;
  final double missingAmount;

  const MinimumOrderBanner({
    super.key,
    required this.minimumOrder,
    required this.missingAmount,
  });

  String _money(double value) {
    if ((value % 1).abs() < 0.0001) return '${value.toStringAsFixed(0)} DA';
    return '${value.toStringAsFixed(2)} DA';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 48,
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.symmetric(horizontal: 12),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: const Color(0xFFFFECEC),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        'Minimum : ${_money(minimumOrder)} - Il vous manque ${_money(missingAmount)}',
        textAlign: TextAlign.center,
        style: const TextStyle(
          color: Color(0xFFB93838),
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
