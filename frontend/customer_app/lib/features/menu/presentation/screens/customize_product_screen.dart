import 'package:flutter/material.dart';

class CustomizeProductScreen extends StatelessWidget {
  const CustomizeProductScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Customize')),
      body: const Center(child: Text('Customize Screen')),
    );
  }
}
