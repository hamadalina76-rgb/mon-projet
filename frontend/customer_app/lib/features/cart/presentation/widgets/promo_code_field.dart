import 'package:flutter/material.dart';

class PromoCodeField extends StatelessWidget {
  final TextEditingController controller;
  final VoidCallback onApply;
  final bool isLoading;

  const PromoCodeField({
    super.key,
    required this.controller,
    required this.onApply,
    required this.isLoading,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: TextField(
            controller: controller,
            textInputAction: TextInputAction.done,
            decoration: const InputDecoration(
              hintText: 'Code promo',
              border: OutlineInputBorder(),
              isDense: true,
            ),
          ),
        ),
        const SizedBox(width: 8),
        SizedBox(
          height: 44,
          child: FilledButton(
            onPressed: isLoading ? null : onApply,
            child: isLoading
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const Text('Appliquer'),
          ),
        ),
      ],
    );
  }
}
