import 'package:flutter/material.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_constants.dart';

/// Bouton de connexion via réseaux sociaux (Google, Facebook)
/// 
/// Design moderne avec logos et couleurs de marque
enum SocialProvider { google, facebook }

class SocialLoginButton extends StatelessWidget {
  final SocialProvider provider;
  final VoidCallback onPressed;
  final bool isLoading;

  const SocialLoginButton({
    super.key,
    required this.provider,
    required this.onPressed,
    this.isLoading = false,
  });

  @override
  Widget build(BuildContext context) {
    final isGoogle = provider == SocialProvider.google;
    
    return SizedBox(
      height: AppConstants.buttonHeightSmall,
      child: ElevatedButton(
        onPressed: isLoading ? null : onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: isGoogle ? Colors.white : AppColors.facebook,
          foregroundColor: isGoogle ? AppColors.textPrimary : Colors.white,
          elevation: 0,
          shadowColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppConstants.borderRadiusMedium),
            side: isGoogle
                ? const BorderSide(color: AppColors.border, width: 1.5)
                : BorderSide.none,
          ),
          padding: const EdgeInsets.symmetric(horizontal: 16),
        ),
        child: isLoading
            ? SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(
                    isGoogle ? AppColors.textPrimary : Colors.white,
                  ),
                ),
              )
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Icône du provider
                  _buildProviderIcon(),
                  const SizedBox(width: 12),
                  // Texte
                  Text(
                    isGoogle ? 'Google' : 'Facebook',
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: isGoogle ? AppColors.textPrimary : Colors.white,
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  Widget _buildProviderIcon() {
    if (provider == SocialProvider.google) {
      // Logo Google (utiliser SVG ou Image si disponible)
      return Image.asset(
        'assets/icons/google_logo.png',
        width: 20,
        height: 20,
        errorBuilder: (context, error, stackTrace) {
          // Fallback si l'image n'existe pas
          return Container(
            width: 20,
            height: 20,
            decoration: const BoxDecoration(
              shape: BoxShape.circle,
              color: AppColors.google,
            ),
            child: const Center(
              child: Text(
                'G',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          );
        },
      );
    } else {
      // Logo Facebook
      return const Icon(
        Icons.facebook,
        color: Colors.white,
        size: 20,
      );
    }
  }
}
