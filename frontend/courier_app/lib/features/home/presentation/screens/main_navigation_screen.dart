import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../config/di/injection_container.dart';
import '../../../../services/notification_service.dart';
import '../../../auth/domain/repositories/auth_repository.dart';
import '../../../orders/presentation/providers/orders_provider.dart';
import '../../../orders/presentation/screens/incoming_offer_screen.dart';
import 'courier_home_screen.dart';
import 'courier_status_screen.dart';
import '../../../profile/presentation/screens/profile_screen.dart';

class MainNavigationScreen extends ConsumerStatefulWidget {
  const MainNavigationScreen({super.key});

  @override
  ConsumerState<MainNavigationScreen> createState() => _MainNavigationScreenState();
}

class _MainNavigationScreenState extends ConsumerState<MainNavigationScreen> {
  int _currentIndex = 0;
  bool _canAccessApp = true;
  bool _isInternalCourier = true;
  StreamSubscription<void>? _profileRefreshedSub;
  bool _offerOverlayVisible = false;
  bool _listenerRegistered = false;

  @override
  void initState() {
    super.initState();
    _loadCourierAccess();
    _profileRefreshedSub = NotificationService().onProfileRefreshed.listen((_) {
      if (mounted) _loadCourierAccess();
    });
  }


  @override
  void dispose() {
    _profileRefreshedSub?.cancel();
    super.dispose();
  }

  Future<void> _loadCourierAccess() async {
    if (!getIt.isRegistered<AuthRepository>()) return;
    try {
      final c = await getIt<AuthRepository>().getCurrentCourier();
      if (!mounted) return;
      final canAccess = c?.canAccessApp ?? true;
      if (c != null && c.isBlocked) {
        context.go('/rejected');
        return;
      }
      if (c != null && c.isPendingApproval) {
        context.go('/pending');
        return;
      }
      setState(() {
        _canAccessApp = canAccess;
        _isInternalCourier = c?.isInternal ?? false;
        if (!_isInternalCourier && _currentIndex == 2) {
          _currentIndex = 0;
        }
        if (!canAccess) _currentIndex = 3;
      });
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    // Listen for incoming DELIVERY_OFFER and show overlay — runs once per state change
    ref.listen<OrdersState>(ordersProvider, (prev, next) {
      if (next.incomingOffer != null &&
          (prev?.incomingOffer == null) &&
          !_offerOverlayVisible) {
        _offerOverlayVisible = true;
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted) return;
          Navigator.of(context)
              .push(PageRouteBuilder(
                opaque: false,
                pageBuilder: (_, __, ___) =>
                    IncomingOfferOverlay(offer: next.incomingOffer!),
              ))
              .whenComplete(() {
            if (mounted) setState(() => _offerOverlayVisible = false);
          });
        });
      }
    });

    final l10n = AppLocalizations.of(context)!;
    final screens = <Widget>[
      CourierHomeScreen(canAccessApp: _canAccessApp),
      const _OrdersTabPlaceholder(),
      if (_isInternalCourier) const CourierStatusScreen(),
      const ProfileScreen(),
    ];

    if (_currentIndex >= screens.length) {
      _currentIndex = screens.length - 1;
    }

    return Scaffold(
      body: screens[_currentIndex],
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.1),
              blurRadius: 10,
              offset: const Offset(0, -2),
            ),
          ],
        ),
        child: SafeArea(
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: 8.w, vertical: 8.h),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildNavItem(
                  icon: Icons.home_outlined,
                  activeIcon: Icons.home,
                  index: 0,
                ),
                _buildNavItem(
                  icon: Icons.receipt_long_outlined,
                  activeIcon: Icons.receipt_long,
                  index: 1,
                ),
                if (_isInternalCourier)
                  _buildNavItem(
                    icon: Icons.event_busy_outlined,
                    activeIcon: Icons.event_busy,
                    index: 2,
                  ),
                _buildNavItem(
                  icon: Icons.person_outline,
                  activeIcon: Icons.person,
                  index: _isInternalCourier ? 3 : 2,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildNavItem({
    required IconData icon,
    required IconData activeIcon,
    required int index,
    bool showBadge = false,
  }) {
    final isActive = _currentIndex == index;
    final isProfile = index == 3;
    final isDisabled = !_canAccessApp && !isProfile;

    return GestureDetector(
      onTap: () {
        if (isDisabled) {
          setState(() => _currentIndex = 3);
          return;
        }
        setState(() => _currentIndex = index);
      },
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.h),
        decoration: BoxDecoration(
          color: isActive ? AppColors.primary.withOpacity(0.1) : Colors.transparent,
          borderRadius: BorderRadius.circular(12.r),
        ),
        child: Icon(
          isActive ? activeIcon : icon,
          color: isActive
              ? AppColors.primary
              : (isDisabled ? Colors.grey[400] : Colors.grey[600]),
          size: 28.sp,
        ),
      ),
    );
  }
}

class _OrdersTabPlaceholder extends ConsumerWidget {
  const _OrdersTabPlaceholder();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ordersState = ref.watch(ordersProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Mes commandes')),
      body: Center(
        child: ordersState.successMessage != null
            ? Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.check_circle, color: Colors.green, size: 60),
                  const SizedBox(height: 12),
                  Text(ordersState.successMessage!,
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                ],
              )
            : const Text('Aucune commande active'),
      ),
    );
  }
}
