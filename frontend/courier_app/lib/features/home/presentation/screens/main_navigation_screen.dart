import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../config/di/injection_container.dart';
import '../../../../services/notification_service.dart';
import '../../../auth/domain/repositories/auth_repository.dart';
import 'courier_home_screen.dart';
import '../../../profile/presentation/screens/profile_screen.dart';

class MainNavigationScreen extends StatefulWidget {
  const MainNavigationScreen({super.key});

  @override
  State<MainNavigationScreen> createState() => _MainNavigationScreenState();
}

class _MainNavigationScreenState extends State<MainNavigationScreen> {
  int _currentIndex = 0;
  bool _canAccessApp = true;
  StreamSubscription<void>? _profileRefreshedSub;

  @override
  void initState() {
    super.initState();
    _loadCourierAccess();
    // Rafraîchir l'accès en temps réel quand une notif "compte approuvé/bloqué" est reçue
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
        if (!canAccess) _currentIndex = 3;
      });
    } catch (_) {}
  }

  List<Widget> get _screens => [
    CourierHomeScreen(canAccessApp: _canAccessApp),
    const Center(child: Text('Orders')), // TODO: Implement OrdersScreen
    const Center(child: Text('Earnings')), // TODO: Implement EarningsScreen
    const ProfileScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _screens[_currentIndex],
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
                _buildNavItem(
                  icon: Icons.attach_money_outlined,
                  activeIcon: Icons.attach_money,
                  index: 2,
                ),
                _buildNavItem(
                  icon: Icons.person_outline,
                  activeIcon: Icons.person,
                  index: 3,
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
  }) {
    final isActive = _currentIndex == index;
    final isProfile = index == 3;
    final isDisabled = !_canAccessApp && !isProfile;

    return GestureDetector(
      onTap: () {
        if (isDisabled) {
          setState(() => _currentIndex = 4);
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
