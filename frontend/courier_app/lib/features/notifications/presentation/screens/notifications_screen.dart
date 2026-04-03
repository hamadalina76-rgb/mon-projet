import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../../../core/theme/app_colors.dart';
import '../providers/notifications_provider.dart';
import '../widgets/notification_item.dart';

class NotificationsScreen extends ConsumerStatefulWidget {
	const NotificationsScreen({super.key});

	@override
	ConsumerState<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
	final ScrollController _scrollController = ScrollController();

	@override
	void initState() {
		super.initState();
		_scrollController.addListener(_onScroll);

		WidgetsBinding.instance.addPostFrameCallback((_) {
			ref.read(notificationsProvider.notifier).loadInitial();
		});
	}

	@override
	void dispose() {
		_scrollController.removeListener(_onScroll);
		_scrollController.dispose();
		super.dispose();
	}

	void _onScroll() {
		if (!_scrollController.hasClients) {
			return;
		}

		final threshold = _scrollController.position.maxScrollExtent - 260;
		if (_scrollController.position.pixels >= threshold) {
			ref.read(notificationsProvider.notifier).loadMore();
		}
	}

	@override
	Widget build(BuildContext context) {
		final state = ref.watch(notificationsProvider);
		final controller = ref.read(notificationsProvider.notifier);
		final visibleItems = state.visibleItems;

		return Scaffold(
			backgroundColor: const Color(0xFFF6F8FB),
			appBar: AppBar(
				elevation: 0,
				backgroundColor: Colors.transparent,
				foregroundColor: const Color(0xFF111827),
				title: Text(
					'Notifications',
					style: TextStyle(
						fontSize: 19.sp,
						fontWeight: FontWeight.w800,
						color: const Color(0xFF111827),
					),
				),
			),
			body: RefreshIndicator(
				onRefresh: controller.refresh,
				child: CustomScrollView(
					controller: _scrollController,
					physics: const AlwaysScrollableScrollPhysics(),
					slivers: [
						SliverToBoxAdapter(
							child: Padding(
								padding: EdgeInsets.fromLTRB(16.w, 2.h, 16.w, 10.h),
								child: Column(
									crossAxisAlignment: CrossAxisAlignment.start,
									children: [
										_TopPanel(
											unreadCount: state.unreadCount,
											onMarkAll: controller.markAllAsRead,
										),
										SizedBox(height: 14.h),
										Row(
											children: [
												ChoiceChip(
													label: const Text('Toutes'),
													selected: state.filter == NotificationFilter.all,
													onSelected: (_) => controller.setFilter(NotificationFilter.all),
													selectedColor: AppColors.primary.withValues(alpha: 0.14),
													side: BorderSide(
														color: state.filter == NotificationFilter.all
																? AppColors.primary
																: const Color(0xFFD8DEE6),
													),
													labelStyle: TextStyle(
														color: state.filter == NotificationFilter.all
																? AppColors.primary
																: const Color(0xFF4B5563),
														fontWeight: FontWeight.w700,
													),
												),
												SizedBox(width: 10.w),
												ChoiceChip(
													label: const Text('Non lues'),
													selected: state.filter == NotificationFilter.unread,
													onSelected: (_) => controller.setFilter(NotificationFilter.unread),
													selectedColor: AppColors.secondary.withValues(alpha: 0.14),
													side: BorderSide(
														color: state.filter == NotificationFilter.unread
																? AppColors.secondary
																: const Color(0xFFD8DEE6),
													),
													labelStyle: TextStyle(
														color: state.filter == NotificationFilter.unread
																? AppColors.secondary
																: const Color(0xFF4B5563),
														fontWeight: FontWeight.w700,
													),
												),
											],
										),
										if (state.error != null) ...[
											SizedBox(height: 12.h),
											Text(
												'Erreur: ${state.error}',
												style: TextStyle(
													color: AppColors.error,
													fontSize: 12.sp,
													fontWeight: FontWeight.w600,
												),
											),
										],
									],
								),
							),
						),
						if (state.loading && visibleItems.isEmpty)
							SliverFillRemaining(
								hasScrollBody: false,
								child: Center(
									child: CircularProgressIndicator(
										color: AppColors.primary,
									),
								),
							)
						else if (visibleItems.isEmpty)
							SliverFillRemaining(
								hasScrollBody: false,
								child: Center(
									child: Padding(
										padding: EdgeInsets.symmetric(horizontal: 24.w),
										child: Column(
											mainAxisAlignment: MainAxisAlignment.center,
											children: [
												Container(
													width: 76.w,
													height: 76.w,
													decoration: const BoxDecoration(
														color: Color(0xFFE9EEF6),
														shape: BoxShape.circle,
													),
													child: Icon(
														Icons.notifications_none_rounded,
														size: 38.sp,
														color: AppColors.secondary,
													),
												),
												SizedBox(height: 16.h),
												Text(
													state.filter == NotificationFilter.unread
															? 'Aucune notification non lue'
															: 'Aucune notification pour le moment',
													textAlign: TextAlign.center,
													style: TextStyle(
														fontSize: 16.sp,
														color: const Color(0xFF344152),
														fontWeight: FontWeight.w700,
													),
												),
												SizedBox(height: 6.h),
												Text(
													'Les nouvelles notifications apparaitront ici.',
													textAlign: TextAlign.center,
													style: TextStyle(
														fontSize: 12.5.sp,
														color: const Color(0xFF7C8794),
													),
												),
											],
										),
									),
								),
							)
						else
							SliverPadding(
								padding: EdgeInsets.fromLTRB(16.w, 0, 16.w, 12.h),
								sliver: SliverList.builder(
									itemCount: visibleItems.length,
									itemBuilder: (context, index) {
										final item = visibleItems[index];
										return Padding(
											padding: EdgeInsets.only(bottom: 10.h),
											child: NotificationItem(
												notification: item,
												onTap: () => controller.markAsRead(item.id),
											),
										);
									},
								),
							),
						SliverToBoxAdapter(
							child: Padding(
								padding: EdgeInsets.fromLTRB(0, 8.h, 0, 30.h),
								child: Center(
									child: state.loadingMore
											? SizedBox(
													width: 22.w,
													height: 22.w,
													child: CircularProgressIndicator(
														strokeWidth: 2.2,
														color: AppColors.primary,
													),
												)
											: const SizedBox.shrink(),
								),
							),
						),
					],
				),
			),
		);
	}
}

class _TopPanel extends StatelessWidget {
	final int unreadCount;
	final VoidCallback onMarkAll;

	const _TopPanel({
		required this.unreadCount,
		required this.onMarkAll,
	});

	@override
	Widget build(BuildContext context) {
		return Container(
			width: double.infinity,
			padding: EdgeInsets.all(14.w),
			decoration: BoxDecoration(
				borderRadius: BorderRadius.circular(18.r),
				gradient: const LinearGradient(
					begin: Alignment.topLeft,
					end: Alignment.bottomRight,
					colors: [
						Color(0xFFE7180B),
						Color(0xFFBA150A),
					],
				),
				boxShadow: [
					BoxShadow(
						color: AppColors.primary.withValues(alpha: 0.32),
						blurRadius: 20,
						offset: const Offset(0, 8),
					),
				],
			),
			child: Row(
				children: [
					Container(
						width: 42.w,
						height: 42.w,
						decoration: BoxDecoration(
							color: Colors.white.withValues(alpha: 0.18),
							borderRadius: BorderRadius.circular(12.r),
						),
						child: Icon(
							Icons.notifications_active_rounded,
							color: Colors.white,
							size: 22.sp,
						),
					),
					SizedBox(width: 12.w),
					Expanded(
						child: Column(
							crossAxisAlignment: CrossAxisAlignment.start,
							children: [
								Text(
									'$unreadCount non lue${unreadCount > 1 ? 's' : ''}',
									style: TextStyle(
										color: Colors.white,
										fontSize: 16.sp,
										fontWeight: FontWeight.w800,
									),
								),
								SizedBox(height: 2.h),
								Text(
									'Restez informe de vos commandes et alertes.',
									style: TextStyle(
										color: Colors.white.withValues(alpha: 0.86),
										fontSize: 12.sp,
										fontWeight: FontWeight.w500,
									),
								),
							],
						),
					),
					if (unreadCount > 0)
						TextButton(
							style: TextButton.styleFrom(
								foregroundColor: Colors.white,
								backgroundColor: Colors.white.withValues(alpha: 0.15),
								shape: RoundedRectangleBorder(
									borderRadius: BorderRadius.circular(999),
								),
							),
							onPressed: onMarkAll,
							child: Text(
								'Tout lire',
								style: TextStyle(
									fontSize: 12.sp,
									fontWeight: FontWeight.w700,
								),
							),
						),
				],
			),
		);
	}
}
