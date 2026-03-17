import 'package:flutter/material.dart';
import '../../../../core/widgets/speedline_app_bar.dart';

class SearchScreen extends StatelessWidget {
  const SearchScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const SpeedlineAppBar(title: 'Search'),
      body: const Center(child: Text('Search Screen')),
    );
  }
}
