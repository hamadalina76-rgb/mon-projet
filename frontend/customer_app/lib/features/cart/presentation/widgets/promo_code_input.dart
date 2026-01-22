import 'package:flutter/material.dart';

class PromoCodeInput extends StatelessWidget {
  const PromoCodeInput({super.key});

  @override
  Widget build(BuildContext context) {
    return const TextField(decoration: InputDecoration(hintText: 'Promo Code'));
  }
}
