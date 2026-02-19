import 'dart:async';
import '../../../../core/constants/app_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:geolocator/geolocator.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/localization/app_localizations.dart';


/// Enable Location Screen
/// Shows after successful login to request location permission
class EnableLocationScreen extends ConsumerStatefulWidget {
  const EnableLocationScreen({super.key});

  @override
  ConsumerState<EnableLocationScreen> createState() =>
      _EnableLocationScreenState();
}

class _EnableLocationScreenState extends ConsumerState<EnableLocationScreen> {
  bool _isLoading = false;

  Future<void> _handleAllowLocation() async {
    setState(() => _isLoading = true);
    final l10n = AppLocalizations.of(context)!;

    try {
      // Check if location services are enabled
      bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        if (mounted) {
          _showError(l10n.translate('location_services_disabled'));
        }
        setState(() => _isLoading = false);
        return;
      }

      // Check permission
      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
        if (permission == LocationPermission.denied) {
          if (mounted) {
            _showError(l10n.translate('location_permission_denied'));
          }
          setState(() => _isLoading = false);
          return;
        }
      }

      if (permission == LocationPermission.deniedForever) {
        if (mounted) {
          _showError(l10n.translate('location_permanently_denied'));
          // Optionally open app settings
          await Geolocator.openAppSettings();
        }
        setState(() => _isLoading = false);
        return;
      }

      // Get current position
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 10),
      );

      // Success! Show position in console (for debugging)
      debugPrint('Location obtained: ${position.latitude}, ${position.longitude}');

      // TODO: Save location to backend
      // For now, just navigate to explore (main home screen)
      if (mounted) {
        context.go(RouteNames.explore);
      }
    } on LocationServiceDisabledException {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        _showError(l10n.translate('location_services_disabled'));
      }
    } on PermissionDeniedException {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        _showError(l10n.translate('location_permission_denied'));
      }
    } on TimeoutException {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        _showError(l10n.translate('location_timeout'));
      }
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        _showError('${l10n.translate('location_error')}: ${e.toString()}');
      }
      debugPrint('Location error: $e');
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  void _handleManualEntry() {
    final l10n = AppLocalizations.of(context)!;
    // TODO: Navigate to manual address entry screen
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.translate('manual_address_entry')),
        content: Text(l10n.translate('manual_address_coming_soon')),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(l10n.translate('ok')),
          ),
        ],
      ),
    );
  }

  void _handleSkip() {
    context.go(RouteNames.explore);
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            children: [
              const Spacer(flex: 2),
              
              // Location Icon Illustration
              Container(
                width: 280,
                height: 280,
                decoration: BoxDecoration(
                  color: AppColors.primary.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    // Outer circle with opacity
                    Container(
                      width: 280,
                      height: 280,
                      decoration: BoxDecoration(
                        border: Border.all(
                          color: AppColors.primary.withOpacity(0.2),
                          width: 2,
                        ),
                        shape: BoxShape.circle,
                      ),
                    ),
                    // Location pin icon
                    Icon(
                      Icons.location_on,
                      size: 120,
                      color: AppColors.primary,
                    ),
                    // Small decorative elements (restaurants)
                    Positioned(
                      top: 40,
                      left: 80,
                      child: Container(
                        width: 50,
                        height: 50,
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.1),
                              blurRadius: 10,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        child: const Icon(
                          Icons.restaurant,
                          color: AppColors.primary,
                          size: 28,
                        ),
                      ),
                    ),
                    Positioned(
                      bottom: 60,
                      right: 60,
                      child: Container(
                        width: 60,
                        height: 60,
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.1),
                              blurRadius: 10,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        child: const Icon(
                          Icons.fastfood,
                          color: AppColors.primary,
                          size: 32,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              
              const SizedBox(height: 48),
              
              // Title
              Text(
                l10n.translate('enable_location'),
                style: const TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
                textAlign: TextAlign.center,
              ),
              
              const SizedBox(height: 16),
              
              // Description
              RichText(
                textAlign: TextAlign.center,
                text: TextSpan(
                  style: const TextStyle(
                    fontSize: 16,
                    color: Colors.black54,
                    height: 1.5,
                  ),
                  children: [
                    TextSpan(
                      text: l10n.translate('location_permission_desc'),
                    ),
                    TextSpan(
                      text: l10n.translate('accurate_delivery'),
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                  ],
                ),
              ),
              
              const Spacer(flex: 2),
              
              // Allow Location Access Button
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _handleAllowLocation,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(28),
                    ),
                    elevation: 0,
                  ),
                  child: _isLoading
                      ? const SizedBox(
                          height: 24,
                          width: 24,
                          child: CircularProgressIndicator(
                            color: Colors.white,
                            strokeWidth: 2,
                          ),
                        )
                      : Text(
                          l10n.translate('allow_location_access'),
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                ),
              ),
              
              const SizedBox(height: 16),
              
              // Enter Address Manually Button
              SizedBox(
                width: double.infinity,
                height: 56,
                child: OutlinedButton.icon(
                  onPressed: _handleManualEntry,
                  icon: const Icon(Icons.edit_location_outlined),
                  label: Text(
                    l10n.translate('enter_address_manually'),
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.primary,
                    side: BorderSide(color: AppColors.primary.withOpacity(0.3)),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(28),
                    ),
                  ),
                ),
              ),
              
              const SizedBox(height: 24),
              
              // Privacy notice
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.lock_outline,
                    size: 16,
                    color: Colors.grey[400],
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Your privacy is our priority. Your data is encrypted.',
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey[500],
                    ),
                  ),
                ],
              ),
              
              const SizedBox(height: 16),
              
              // Skip for now
              TextButton(
                onPressed: _handleSkip,
                child: Text(
                  l10n.translate('skip_for_now'),
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey[600],
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }
}
