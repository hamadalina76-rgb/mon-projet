import 'package:flutter/material.dart';

class AuthTextField extends StatelessWidget {
  final String label;

  const AuthTextField({super.key, required this.label});

  @override
  Widget build(BuildContext context) {
    return TextField(
      decoration: InputDecoration(labelText: label),
    );
  }
}
