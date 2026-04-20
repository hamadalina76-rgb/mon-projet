import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';

class OrderStatusStyle {
  final Color background;
  final Color foreground;
  final Color border;
  final Color dot;

  const OrderStatusStyle({
    required this.background,
    required this.foreground,
    required this.border,
    required this.dot,
  });
}

class OrderStatusPalette {
  static const Color _warningDark = Color(0xFFD97706);
  static const Color _infoDark = Color(0xFF2563EB);
  static const Color _violet = Color(0xFF8B5CF6);
  static const Color _violetDark = Color(0xFF6D28D9);
  static const Color _successDark = Color(0xFF059669);
  static const Color _errorDark = Color(0xFFDC2626);
  static const Color _gray100 = Color(0xFFEDF2F7);
  static const Color _gray200 = Color(0xFFE2E8F0);
  static const Color _gray400 = Color(0xFFA0AEC0);
  static const Color _gray600 = Color(0xFF4A5568);

  static String normalizeStatus(String rawStatus) {
    final normalized = rawStatus.trim().toUpperCase();
    if (normalized == 'READY_FOR_PICKUP') return 'READY';
    return normalized;
  }

  static OrderStatusStyle forStatus(String rawStatus) {
    switch (normalizeStatus(rawStatus)) {
      case 'PENDING':
        return OrderStatusStyle(
          background: AppColors.warning.withValues(alpha: 0.12),
          foreground: _warningDark,
          border: AppColors.warning.withValues(alpha: 0.25),
          dot: AppColors.warning,
        );
      case 'CONFIRMED':
        return OrderStatusStyle(
          background: AppColors.info.withValues(alpha: 0.10),
          foreground: _infoDark,
          border: AppColors.info.withValues(alpha: 0.20),
          dot: AppColors.info,
        );
      case 'PREPARING':
        return OrderStatusStyle(
          background: _violet.withValues(alpha: 0.10),
          foreground: _violetDark,
          border: _violet.withValues(alpha: 0.20),
          dot: _violet,
        );
      case 'READY':
      case 'IN_DELIVERY':
        return OrderStatusStyle(
          background: AppColors.success.withValues(alpha: 0.10),
          foreground: _successDark,
          border: AppColors.success.withValues(alpha: 0.20),
          dot: AppColors.success,
        );
      case 'PICKED_UP':
        return const OrderStatusStyle(
          background: _gray100,
          foreground: _gray600,
          border: _gray200,
          dot: _gray400,
        );
      case 'DELIVERED':
        return OrderStatusStyle(
          background: AppColors.success.withValues(alpha: 0.08),
          foreground: _successDark,
          border: AppColors.success.withValues(alpha: 0.15),
          dot: AppColors.success,
        );
      case 'CANCELLED':
        return OrderStatusStyle(
          background: AppColors.error.withValues(alpha: 0.08),
          foreground: _errorDark,
          border: AppColors.error.withValues(alpha: 0.15),
          dot: AppColors.error,
        );
      default:
        return OrderStatusStyle(
          background: AppColors.surfaceLight,
          foreground: AppColors.textSecondary,
          border: AppColors.border,
          dot: AppColors.secondaryGrey,
        );
    }
  }
}