import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'config/di/injection_container.dart';
import 'config/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'core/localization/app_localizations.dart';
import 'core/localization/locale_config.dart';
import 'core/localization/locale_provider.dart';
import 'features/auth/domain/repositories/auth_repository.dart';
import 'services/notification_service.dart';

class CourierApp extends ConsumerWidget {
  const CourierApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    final locale = ref.watch(localeProvider);
    // Réaction en temps réel aux notifications (compte approuvé / bloqué)
    NotificationService().setCourierActionHandler((action) async {
      if (!getIt.isRegistered<AuthRepository>()) return;
      try {
        await getIt<AuthRepository>().fetchCourierProfile();
      } catch (_) {}
      if (action == kActionCourierSuspended ||
          action == kActionCourierRejected ||
          action == kActionCourierDeactivated) {
        router.go('/rejected');
      } else if (action == kActionCourierApproved ||
          action == kActionCourierReactivated) {
        router.go('/home');
      }
    });
    
    return ScreenUtilInit(
      designSize: const Size(375, 812), // iPhone 11 Pro
      minTextAdapt: true,
      splitScreenMode: true,
      builder: (context, child) {
        return MaterialApp.router(
          title: 'SpeedLine Courier',
          debugShowCheckedModeBanner: false,
          
          // THEME
          theme: AppTheme.lightTheme,
          darkTheme: AppTheme.darkTheme,
          themeMode: ThemeMode.light,
          
          // ROUTING
          routerConfig: router,
          
          // LOCALIZATION
          localizationsDelegates: const [
            AppLocalizations.delegate,
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          supportedLocales: const [
            ...supportedLocales,
          ],
          locale: locale,
          
          // BUILDER
          builder: (context, widget) {
            return MediaQuery(
              data: MediaQuery.of(context).copyWith(
                textScaler: const TextScaler.linear(1.0),
              ),
              child: widget!,
            );
          },
        );
      },
    );
  }
}
