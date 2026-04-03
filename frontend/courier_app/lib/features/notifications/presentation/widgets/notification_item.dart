import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../../../core/theme/app_colors.dart';
import '../../domain/entities/notification.dart';

class NotificationItem extends StatelessWidget {
	final AppNotification notification;
	final VoidCallback? onTap;

	const NotificationItem({
		super.key,
		required this.notification,
		this.onTap,
	});

	@override
	Widget build(BuildContext context) {
		final palette = _styleByType(notification.type);

		return Material(
			color: Colors.transparent,
			child: InkWell(
				borderRadius: BorderRadius.circular(18.r),
				onTap: onTap,
				child: Ink(
					padding: EdgeInsets.all(14.w),
					decoration: BoxDecoration(
						borderRadius: BorderRadius.circular(18.r),
						gradient: LinearGradient(
							begin: Alignment.topLeft,
							end: Alignment.bottomRight,
							colors: [
								palette.background,
								Colors.white,
							],
						),
						border: Border.all(
							color: notification.isRead
									? const Color(0xFFE7EBEF)
									: palette.primary.withValues(alpha: 0.28),
							width: notification.isRead ? 1 : 1.3,
						),
						boxShadow: [
							BoxShadow(
								color: Colors.black.withValues(alpha: 0.04),
								blurRadius: 16,
								offset: const Offset(0, 8),
							),
						],
					),
					child: Row(
						crossAxisAlignment: CrossAxisAlignment.start,
						children: [
							Container(
								width: 42.w,
								height: 42.w,
								decoration: BoxDecoration(
									color: palette.primary.withValues(alpha: 0.15),
									borderRadius: BorderRadius.circular(12.r),
								),
								child: Icon(
									palette.icon,
									color: palette.primary,
									size: 22.sp,
								),
							),
							SizedBox(width: 12.w),
							Expanded(
								child: Column(
									crossAxisAlignment: CrossAxisAlignment.start,
									children: [
										Row(
											children: [
												Expanded(
													child: Text(
														notification.title,
														maxLines: 1,
														overflow: TextOverflow.ellipsis,
														style: TextStyle(
															fontSize: 14.sp,
															fontWeight: FontWeight.w700,
															color: const Color(0xFF1B1F24),
														),
													),
												),
												if (!notification.isRead)
													Container(
														padding: EdgeInsets.symmetric(
															horizontal: 8.w,
															vertical: 3.h,
														),
														decoration: BoxDecoration(
															color: AppColors.primary.withValues(alpha: 0.13),
															borderRadius: BorderRadius.circular(999),
														),
														child: Text(
															'Nouveau',
															style: TextStyle(
																color: AppColors.primary,
																fontSize: 10.sp,
																fontWeight: FontWeight.w700,
															),
														),
													),
											],
										),
										SizedBox(height: 5.h),
										Text(
											notification.message,
											maxLines: 2,
											overflow: TextOverflow.ellipsis,
											style: TextStyle(
												fontSize: 12.5.sp,
												height: 1.35,
												color: const Color(0xFF525F6B),
											),
										),
										SizedBox(height: 10.h),
										Text(
											_relativeTime(notification.timestamp),
											style: TextStyle(
												fontSize: 11.sp,
												fontWeight: FontWeight.w600,
												color: const Color(0xFF8B97A3),
											),
										),
									],
								),
							),
						],
					),
				),
			),
		);
	}

	String _relativeTime(DateTime value) {
		final now = DateTime.now();
		final diff = now.difference(value);

		if (diff.inSeconds < 60) return 'A l\'instant';
		if (diff.inMinutes < 60) return 'Il y a ${diff.inMinutes} min';
		if (diff.inHours < 24) return 'Il y a ${diff.inHours} h';
		if (diff.inDays == 1) return 'Hier';
		if (diff.inDays < 7) return 'Il y a ${diff.inDays} jours';

		final d = value.toLocal();
		final dd = d.day.toString().padLeft(2, '0');
		final mm = d.month.toString().padLeft(2, '0');
		final yyyy = d.year.toString();
		return '$dd/$mm/$yyyy';
	}

	_NotificationPalette _styleByType(NotificationType type) {
		return switch (type) {
			NotificationType.newOrder => const _NotificationPalette(
					icon: Icons.receipt_long_rounded,
					primary: Color(0xFFE77016),
					background: Color(0xFFFFF3E8),
				),
			NotificationType.orderCancelled => const _NotificationPalette(
					icon: Icons.cancel_outlined,
					primary: Color(0xFFB3261E),
					background: Color(0xFFFFECEB),
				),
			NotificationType.message => const _NotificationPalette(
					icon: Icons.forum_outlined,
					primary: Color(0xFF1976D2),
					background: Color(0xFFEAF4FF),
				),
			NotificationType.earning => const _NotificationPalette(
					icon: Icons.account_balance_wallet_outlined,
					primary: Color(0xFF2E7D32),
					background: Color(0xFFEAF8EC),
				),
			NotificationType.bonus => const _NotificationPalette(
					icon: Icons.workspace_premium_outlined,
					primary: Color(0xFF7B1FA2),
					background: Color(0xFFF6EDFC),
				),
			NotificationType.system => const _NotificationPalette(
					icon: Icons.notifications_active_outlined,
					primary: AppColors.secondary,
					background: Color(0xFFEAF0F8),
				),
		};
	}
}

class _NotificationPalette {
	final IconData icon;
	final Color primary;
	final Color background;

	const _NotificationPalette({
		required this.icon,
		required this.primary,
		required this.background,
	});
}
