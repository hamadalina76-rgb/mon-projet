import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../config/di/injection_container.dart';
import '../../domain/repositories/auth_repository.dart';

/// Shown when courier status is PENDING_APPROVAL. App access is blocked until admin approves.
class PendingApprovalScreen extends StatelessWidget {
  const PendingApprovalScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.symmetric(horizontal: 24.w),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.schedule,
                size: 80.sp,
                color: AppColors.primary,
              ),
              SizedBox(height: 24.h),
              Text(
                'Inscription en cours d\'examen',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 22.sp,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
              SizedBox(height: 16.h),
              Text(
                'Votre inscription est en cours d\'examen par notre équipe. Vous serez notifié dès que votre compte sera activé.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 16.sp,
                  color: Colors.black54,
                  height: 1.4,
                ),
              ),
              SizedBox(height: 12.h),
              Text(
                'Si nous avons demandé des informations complémentaires, vous pouvez modifier votre profil et vos documents ci‑dessous.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 14.sp,
                  color: Colors.black45,
                  height: 1.3,
                ),
              ),
              SizedBox(height: 24.h),
              FilledButton.icon(
                onPressed: () => context.push('/documentation'),
                icon: const Icon(Icons.edit_document, size: 20),
                label: const Text('Compléter ou modifier mon profil'),
                style: FilledButton.styleFrom(
                  padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 14.h),
                  backgroundColor: AppColors.primary,
                ),
              ),
              SizedBox(height: 16.h),
              OutlinedButton.icon(
                onPressed: () async {
                  if (!getIt.isRegistered<AuthRepository>()) return;
                  final authRepository = getIt<AuthRepository>();
                  try {
                    final profile = await authRepository.fetchCourierProfile();
                    if (context.mounted) {
                      if (profile.canAccessApp) {
                        context.go('/home');
                      } else if (profile.isBlocked) {
                        context.go('/rejected');
                      }
                    }
                  } catch (_) {}
                },
                icon: const Icon(Icons.refresh),
                label: const Text('Rafraîchir le statut'),
                style: OutlinedButton.styleFrom(
                  padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 12.h),
                ),
              ),
              SizedBox(height: 48.h),
              TextButton(
                onPressed: () => context.go('/login'),
                child: const Text('Se déconnecter'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
