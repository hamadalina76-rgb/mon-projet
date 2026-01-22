import 'package:flutter/material.dart';

class MenuScreen extends StatelessWidget {
  final String partnerId;

  const MenuScreen({super.key, required this.partnerId});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Menu')),
      body: const Center(child: Text('Menu Screen')),
    );
  }
}
