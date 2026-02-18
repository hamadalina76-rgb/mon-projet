import 'package:flutter/material.dart';
import '../../../../core/constants/app_colors.dart';

/// Explore Screen
/// Main screen for browsing restaurants and food options
class ExploreScreen extends StatelessWidget {
  const ExploreScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Explore'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.explore_outlined,
              size: 80,
              color: AppColors.textSecondary,
            ),
            SizedBox(height: 16),
            Text(
              'Explore Screen',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: AppColors.textPrimary,
              ),
            ),
            SizedBox(height: 8),
            Text(
              'Coming soon...',
              style: TextStyle(
                fontSize: 16,
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
