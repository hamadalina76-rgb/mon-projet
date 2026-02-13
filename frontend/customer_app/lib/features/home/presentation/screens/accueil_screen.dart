import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/utils/responsive_utils.dart';
import '../../../../core/localization/localization_extension.dart';

/// Écran d'accueil principal de l'application SpeedLine
/// 
/// Affiché après une authentification réussie
class AccueilScreen extends ConsumerWidget {
  const AccueilScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.primary,
        elevation: 0,
        title: Text(
          context.tr('app_name'),
          style: TextStyle(
            fontSize: ResponsiveUtils.getResponsiveFontSize(context, 20),
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
        centerTitle: true,
      ),
      body: Center(
        child: Padding(
          padding: EdgeInsets.all(ResponsiveUtils.getResponsiveSpacing(context, 24)),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Icon de succès
              Container(
                width: ResponsiveUtils.getResponsiveSize(context, 120),
                height: ResponsiveUtils.getResponsiveSize(context, 120),
                decoration: BoxDecoration(
                  color: AppColors.success.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.check_circle_outline,
                  size: ResponsiveUtils.getResponsiveSize(context, 60),
                  color: AppColors.success,
                ),
              ),
              
              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 32)),
              
              // Titre
              Text(
                context.tr('welcome_speedline'),
                style: TextStyle(
                  fontSize: ResponsiveUtils.getResponsiveFontSize(context, 28),
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
                textAlign: TextAlign.center,
              ),
              
              SizedBox(height: ResponsiveUtils.getResponsiveSpacing(context, 16)),
              
              // Description
              Text(
                context.tr('welcome_message'),
                style: TextStyle(
                  fontSize: ResponsiveUtils.getResponsiveFontSize(context, 16),
                  color: AppColors.textSecondary,
                  height: 1.5,
                ),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
