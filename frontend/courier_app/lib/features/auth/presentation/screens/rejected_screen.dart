import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../config/di/injection_container.dart';
import '../../domain/repositories/auth_repository.dart';

/// Shown when courier status is REJECTED or SUSPENDED. App access is blocked.
class RejectedScreen extends StatelessWidget {
  const RejectedScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 24.h),
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: constraints.maxHeight - 48.h),
                child: IntrinsicHeight(
                  child: FutureBuilder<String?>(
                    future: _loadRejectionReason(),
                    builder: (context, snapshot) {
                      final reason = snapshot.data;
                      return Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.block,
                            size: 80.sp,
                            color: Colors.red.shade700,
                          ),
                          SizedBox(height: 24.h),
                          Text(
                            'Compte non autorisé',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 22.sp,
                              fontWeight: FontWeight.bold,
                              color: Colors.black87,
                            ),
                          ),
                          SizedBox(height: 16.h),
                          Text(
                            reason != null && reason.isNotEmpty
                                ? reason
                                : 'Votre compte n\'est pas autorisé à utiliser l\'application. En cas de question, contactez le support.',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 16.sp,
                              color: Colors.black54,
                              height: 1.4,
                            ),
                          ),
                          SizedBox(height: 32.h),
                          OutlinedButton.icon(
                            onPressed: () async {
                              if (!getIt.isRegistered<AuthRepository>()) return;
                              final authRepository = getIt<AuthRepository>();
                              try {
                                final profile = await authRepository.fetchCourierProfile();
                                if (context.mounted) {
                                  if (profile.canAccessApp) {
                                    context.go('/home');
                                  } else if (profile.isPendingApproval) {
                                    context.go('/pending');
                                  }
                                }
                              } catch (_) {}
                            },
                            icon: Icon(Icons.refresh, size: 20.sp),
                            label: Text('Rafraîchir le statut', style: TextStyle(fontSize: 15.sp)),
                            style: OutlinedButton.styleFrom(
                              padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 14.h),
                              minimumSize: Size(double.infinity, 48.h),
                            ),
                          ),
                          SizedBox(height: 24.h),
                          TextButton(
                            onPressed: () => context.go('/login'),
                            child: Text('Se déconnecter', style: TextStyle(fontSize: 15.sp)),
                          ),
                        ],
                      );
                    },
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  Future<String?> _loadRejectionReason() async {
    if (!getIt.isRegistered<AuthRepository>()) return null;
    try {
      final profile = await getIt<AuthRepository>().fetchCourierProfile();
      return profile.rejectionReason ?? profile.suspensionReason;
    } catch (_) {
      return null;
    }
  }
}
