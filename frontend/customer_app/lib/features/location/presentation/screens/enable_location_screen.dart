// This file has been replaced — see full implementation below
// ignore_for_file: unused_import
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:location/location.dart' as loc;
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../data/models/saved_location.dart';
import '../providers/location_provider.dart';

/// Écran d'activation de la localisation
///
/// Flux complet selon les critères d'acceptation:
/// 1. Demander la permission GPS
/// 2. Si acceptée → détecter position → géocodage inverse → écran de confirmation
/// 3. Si refusée → mode saisie manuelle
/// 4. GPS désactivé → fallback vers saisie manuelle
class EnableLocationScreen extends ConsumerStatefulWidget {
  const EnableLocationScreen({super.key});

  @override
  ConsumerState<EnableLocationScreen> createState() =>
      _EnableLocationScreenState();
}

class _EnableLocationScreenState extends ConsumerState<EnableLocationScreen>
    with SingleTickerProviderStateMixin {
  bool _isLoading = false;

  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat(reverse: true);
    _pulseAnimation = Tween<double>(begin: 0.95, end: 1.05).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.dark,
      ),
    );
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  // ──────────────────────────────────────────────────────── GPS Flow ─────────

  Future<void> _handleAllowLocation() async {
    if (_isLoading) return;
    setState(() => _isLoading = true);
    ref.read(locationNotifierProvider.notifier).setLoading();

    try {
      // 1. GPS activé? → show native system dialog if not
      if (!await Geolocator.isLocationServiceEnabled()) {
        final enabled = await loc.Location().requestService();
        if (!enabled || !mounted) {
          ref.read(locationNotifierProvider.notifier).setDenied();
          if (mounted) setState(() => _isLoading = false);
          return;
        }
      }

      // 2. Permission accordée?
      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }

      if (permission == LocationPermission.denied) {
        ref.read(locationNotifierProvider.notifier).setDenied();
        return;
      }

      if (permission == LocationPermission.deniedForever) {
        ref.read(locationNotifierProvider.notifier).setDenied();
        _showSettingsDialog();
        setState(() => _isLoading = false);
        return;
      }

      // 3. Obtenir position GPS
      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 12),
      );
      debugPrint('GPS: ${position.latitude}, ${position.longitude}');

      // 4. Reverse geocode via API
      final notifier = ref.read(locationNotifierProvider.notifier);
      final location = await notifier.reverseGeocode(
        latitude: position.latitude,
        longitude: position.longitude,
      );

      // 5. Persist to Hive and go straight to Explore (no map confirmation step)
      final finalLocation = location ??
          SavedLocation(
            latitude: position.latitude,
            longitude: position.longitude,
            formattedAddress: '',
            savedAt: DateTime.now(),
          );
      await ref
          .read(locationNotifierProvider.notifier)
          .confirmLocation(finalLocation);
      if (mounted) {
        context.go(RouteNames.explore);
      }
    } on LocationServiceDisabledException {
      ref.read(locationNotifierProvider.notifier).setDenied();
    } on PermissionDeniedException {
      ref.read(locationNotifierProvider.notifier).setDenied();
    } on TimeoutException {
      ref.read(locationNotifierProvider.notifier).setDenied();
    } catch (e) {
      debugPrint('Location error: $e');
      ref.read(locationNotifierProvider.notifier).setDenied();
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showSettingsDialog() {
    if (!mounted) return;
    final l10n = AppLocalizations.of(context)!;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text(l10n.translate('location_permanently_denied'),
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        content: Text(l10n.translate('open_settings_for_location')),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(l10n.translate('cancel'),
                style: TextStyle(color: Colors.grey[600])),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(ctx);
              Geolocator.openAppSettings();
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8)),
            ),
            child: Text(l10n.translate('open_settings')),
          ),
        ],
      ),
    );
  }

  void _handleSkip() => context.go(RouteNames.explore);

  // ────────────────────────────────────────────────────────── Build ─────────

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: _buildGpsPromptMode(l10n),
      ),
    );
  }

  // ── GPS prompt ──

  Widget _buildGpsPromptMode(AppLocalizations l10n) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          const Spacer(flex: 2),

          // Illustration
          ScaleTransition(
            scale: _pulseAnimation,
            child: Container(
              width: 200,
              height: 200,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.primary.withOpacity(0.08),
                border: Border.all(
                    color: AppColors.primary.withOpacity(0.15), width: 2),
              ),
              child: Stack(
                alignment: Alignment.center,
                children: [
                  Container(
                    width: 160,
                    height: 160,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: AppColors.primary.withOpacity(0.1),
                    ),
                  ),
                  Icon(Icons.location_on, size: 80, color: AppColors.primary),
                  Positioned(
                      top: 28,
                      left: 28,
                      child: _FloatingIcon(Icons.restaurant, AppColors.primary)),
                  Positioned(
                      bottom: 28,
                      right: 28,
                      child: _FloatingIcon(
                          Icons.local_grocery_store, Colors.green)),
                  Positioned(
                      top: 28,
                      right: 28,
                      child: _FloatingIcon(Icons.fastfood, Colors.orange)),
                ],
              ),
            ),
          ),

          const SizedBox(height: 36),

          Text(
            l10n.translate('enable_location'),
            style: const TextStyle(
                fontSize: 26,
                fontWeight: FontWeight.bold,
                color: AppColors.textPrimary),
            textAlign: TextAlign.center,
          ),

          const SizedBox(height: 12),

          Text(
            l10n.translate('location_permission_desc'),
            style: const TextStyle(
                fontSize: 15, color: AppColors.textSecondary, height: 1.5),
            textAlign: TextAlign.center,
          ),

          const Spacer(flex: 2),

          // Bouton GPS
          SizedBox(
            width: double.infinity,
            height: 54,
            child: ElevatedButton.icon(
              onPressed: _isLoading ? null : _handleAllowLocation,
              icon: _isLoading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                          color: Colors.white, strokeWidth: 2))
                  : const Icon(Icons.gps_fixed),
              label: Text(
                _isLoading
                    ? l10n.translate('detecting_location')
                    : l10n.translate('allow_location_access'),
                style: const TextStyle(
                    fontSize: 16, fontWeight: FontWeight.w600),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(27)),
                elevation: 0,
              ),
            ),
          ),

          const SizedBox(height: 16),

          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.lock_outline, size: 14, color: Colors.grey[400]),
              const SizedBox(width: 6),
              Text(
                l10n.translate('privacy_location_note'),
                style: TextStyle(fontSize: 12, color: Colors.grey[400]),
              ),
            ],
          ),

          const SizedBox(height: 4),

          TextButton(
            onPressed: _handleSkip,
            child: Text(l10n.translate('skip_for_now'),
                style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey[500],
                    fontWeight: FontWeight.w500)),
          ),

          const SizedBox(height: 16),
        ],
      ),
    );
  }

}

/// Petit icône flottant dans l'illustration GPS
class _FloatingIcon extends StatelessWidget {
  final IconData icon;
  final Color color;

  const _FloatingIcon(this.icon, this.color);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 34,
      height: 34,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.08),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Icon(icon, color: color, size: 18),
    );
  }
}

