import 'dart:async';
import '../../../../core/constants/app_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:geolocator/geolocator.dart';

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

    try {
      // Check if location services are enabled
      bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        if (mounted) {
          _showError('Location services are disabled. Please enable them in settings.');
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
            _showError('Location permission denied. Please allow access to continue.');
          }
          setState(() => _isLoading = false);
          return;
        }
      }

      if (permission == LocationPermission.deniedForever) {
        if (mounted) {
          _showError(
              'Location permissions are permanently denied. Please enable them in app settings.');
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
        context.go('/explore');
      }
    } on LocationServiceDisabledException {
      if (mounted) {
        _showError('Location services are disabled. Please enable them in settings.');
      }
    } on PermissionDeniedException {
      if (mounted) {
        _showError('Location permission denied. Please allow access to continue.');
      }
    } on TimeoutException {
      if (mounted) {
        _showError('Location request timed out. Please try again.');
      }
    } catch (e) {
      if (mounted) {
        _showError('Failed to get location: ${e.toString()}');
      }
      debugPrint('Location error: $e');
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  void _handleManualEntry() {
    // TODO: Navigate to manual address entry screen
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Manual Address Entry'),
        content: const Text('Manual address entry screen coming soon!'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  void _handleSkip() {
    context.go('/explore');
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
              const Text(
                'Enable Location',
                style: TextStyle(
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
                text: const TextSpan(
                  style: TextStyle(
                    fontSize: 16,
                    color: Colors.black54,
                    height: 1.5,
                  ),
                  children: [
                    TextSpan(
                      text:
                          'To find the best restaurants near you, we need your precise location. This helps us discover local favorites within a ',
                    ),
                    TextSpan(
                      text: '5 km radius',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    TextSpan(text: '.'),
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
                      : const Text(
                          'Allow Location Access',
                          style: TextStyle(
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
                  label: const Text(
                    'Enter Address Manually',
                    style: TextStyle(
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
                  'Skip for now',
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
