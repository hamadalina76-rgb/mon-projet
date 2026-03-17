import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

class TrackingPermissionResult {
  final bool granted;
  final String message;
  final bool permanentlyDenied;

  const TrackingPermissionResult({
    required this.granted,
    required this.message,
    required this.permanentlyDenied,
  });
}

class PermissionUtils {
  static Future<TrackingPermissionResult> requestTrackingPermissionWithDialog(
      BuildContext context) async {
    final proceed = await showDialog<bool>(
          context: context,
          builder: (context) {
            return AlertDialog(
              title: const Text('Autoriser le suivi de position'),
              content: const Text(
                'SpeedLine a besoin de votre position en continu (meme en arriere-plan) '
                'pour vous assigner les commandes proches et suivre la livraison en temps reel.',
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(false),
                  child: const Text('Annuler'),
                ),
                FilledButton(
                  onPressed: () => Navigator.of(context).pop(true),
                  child: const Text('Continuer'),
                ),
              ],
            );
          },
        ) ??
        false;

    if (!proceed) {
      return const TrackingPermissionResult(
        granted: false,
        message: 'Permission requise pour passer en ligne.',
        permanentlyDenied: false,
      );
    }

    final whenInUse = await Permission.locationWhenInUse.request();
    if (!whenInUse.isGranted) {
      return TrackingPermissionResult(
        granted: false,
        message: whenInUse.isPermanentlyDenied
            ? 'Permission de localisation refusee de facon permanente. Activez-la dans les reglages.'
            : 'Permission de localisation refusee.',
        permanentlyDenied: whenInUse.isPermanentlyDenied,
      );
    }

    final always = await Permission.locationAlways.request();
    if (!always.isGranted) {
      return TrackingPermissionResult(
        granted: false,
        message: always.isPermanentlyDenied
            ? 'Permission "Toujours" refusee de facon permanente. Activez-la dans les reglages.'
            : 'Permission "Toujours" requise pour le tracking en arriere-plan.',
        permanentlyDenied: always.isPermanentlyDenied,
      );
    }

    return const TrackingPermissionResult(
      granted: true,
      message: '',
      permanentlyDenied: false,
    );
  }

  static Future<bool> requestLocationPermission() async {
    final status = await Permission.locationWhenInUse.request();
    return status.isGranted;
  }
  
  static Future<bool> requestCameraPermission() async {
    final status = await Permission.camera.request();
    return status.isGranted;
  }
  
  static Future<bool> requestStoragePermission() async {
    final status = await Permission.storage.request();
    return status.isGranted;
  }
  
  static Future<bool> requestNotificationPermission() async {
    final status = await Permission.notification.request();
    return status.isGranted;
  }
}
