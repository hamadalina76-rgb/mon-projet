import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../config/di/injection_container.dart';
import '../../../../core/api/api_client.dart';
import '../../../auth/domain/repositories/auth_repository.dart';
import '../../data/datasources/notifications_remote_datasource.dart';
import '../../data/repositories/notifications_repository_impl.dart';
import '../../domain/entities/notification.dart';
import '../../domain/repositories/notifications_repository.dart';

enum NotificationFilter {
	all,
	unread,
}

class NotificationsState {
	final List<AppNotification> items;
	final int unreadCount;
	final bool loading;
	final bool refreshing;
	final bool loadingMore;
	final bool hasMore;
	final String? error;
	final int page;
	final bool hasLoadedInitial;
	final NotificationFilter filter;

	const NotificationsState({
		required this.items,
		required this.unreadCount,
		required this.loading,
		required this.refreshing,
		required this.loadingMore,
		required this.hasMore,
		required this.error,
		required this.page,
		required this.hasLoadedInitial,
		required this.filter,
	});

	static const initial = NotificationsState(
		items: [],
		unreadCount: 0,
		loading: false,
		refreshing: false,
		loadingMore: false,
		hasMore: true,
		error: null,
		page: 0,
		hasLoadedInitial: false,
		filter: NotificationFilter.all,
	);

	NotificationsState copyWith({
		List<AppNotification>? items,
		int? unreadCount,
		bool? loading,
		bool? refreshing,
		bool? loadingMore,
		bool? hasMore,
		String? error,
		bool clearError = false,
		int? page,
		bool? hasLoadedInitial,
		NotificationFilter? filter,
	}) {
		return NotificationsState(
			items: items ?? this.items,
			unreadCount: unreadCount ?? this.unreadCount,
			loading: loading ?? this.loading,
			refreshing: refreshing ?? this.refreshing,
			loadingMore: loadingMore ?? this.loadingMore,
			hasMore: hasMore ?? this.hasMore,
			error: clearError ? null : (error ?? this.error),
			page: page ?? this.page,
			hasLoadedInitial: hasLoadedInitial ?? this.hasLoadedInitial,
			filter: filter ?? this.filter,
		);
	}

	List<AppNotification> get visibleItems {
		if (filter == NotificationFilter.all) {
			return items;
		}
		return items.where((e) => !e.isRead).toList();
	}
}

final notificationsRepositoryProvider = Provider<NotificationsRepository>((ref) {
	final dio = getIt<ApiClient>().dio;
	return NotificationsRepositoryImpl(
		remoteDataSource: NotificationsRemoteDataSourceImpl(dio: dio),
	);
});

class NotificationsController extends Notifier<NotificationsState> {
	static const int _pageSize = 20;

	NotificationsRepository get _repository => ref.read(notificationsRepositoryProvider);

	@override
	NotificationsState build() {
		unawaited(loadUnreadCount());
		return NotificationsState.initial;
	}

	Future<int?> _resolveUserId() async {
		if (!getIt.isRegistered<AuthRepository>()) {
			return null;
		}

		final courier = await getIt<AuthRepository>().getCurrentCourier();
		final raw = courier?.userId;
		if (raw == null || raw.isEmpty) {
			return null;
		}

		return int.tryParse(raw);
	}

	Future<void> loadUnreadCount() async {
		try {
			final userId = await _resolveUserId();
			if (userId == null) {
				return;
			}
			final unread = await _repository.getUnreadCount(userId: userId);
			state = state.copyWith(unreadCount: unread);
		} catch (_) {
			// Keep silent to avoid blocking home rendering for badge errors.
		}
	}

	Future<void> loadInitial({bool forceRefresh = false}) async {
		if (state.loading || state.refreshing) {
			return;
		}

		if (state.hasLoadedInitial && !forceRefresh) {
			return;
		}

		state = state.copyWith(loading: true, clearError: true);

		try {
			final userId = await _resolveUserId();
			if (userId == null) {
				throw Exception('Utilisateur introuvable');
			}

			final results = await _repository.fetchNotifications(
				userId: userId,
				page: 0,
				size: _pageSize,
			);
			final unread = await _repository.getUnreadCount(userId: userId);

			state = state.copyWith(
				loading: false,
				hasLoadedInitial: true,
				page: 0,
				items: results,
				unreadCount: unread,
				hasMore: results.length >= _pageSize,
			);
		} catch (e) {
			state = state.copyWith(
				loading: false,
				error: e.toString(),
				hasLoadedInitial: true,
			);
		}
	}

	Future<void> refresh() async {
		if (state.refreshing) {
			return;
		}

		state = state.copyWith(refreshing: true, clearError: true);

		try {
			final userId = await _resolveUserId();
			if (userId == null) {
				throw Exception('Utilisateur introuvable');
			}

			final results = await _repository.fetchNotifications(
				userId: userId,
				page: 0,
				size: _pageSize,
			);
			final unread = await _repository.getUnreadCount(userId: userId);

			state = state.copyWith(
				refreshing: false,
				page: 0,
				items: results,
				unreadCount: unread,
				hasMore: results.length >= _pageSize,
			);
		} catch (e) {
			state = state.copyWith(
				refreshing: false,
				error: e.toString(),
			);
		}
	}

	Future<void> loadMore() async {
		if (state.loading || state.refreshing || state.loadingMore || !state.hasMore) {
			return;
		}

		state = state.copyWith(loadingMore: true, clearError: true);

		try {
			final userId = await _resolveUserId();
			if (userId == null) {
				throw Exception('Utilisateur introuvable');
			}

			final nextPage = state.page + 1;
			final results = await _repository.fetchNotifications(
				userId: userId,
				page: nextPage,
				size: _pageSize,
			);

			final merged = <AppNotification>[...state.items, ...results];
			final deduped = <String, AppNotification>{};
			for (final item in merged) {
				deduped[item.id] = item;
			}

			state = state.copyWith(
				loadingMore: false,
				page: nextPage,
				items: deduped.values.toList()
					..sort((a, b) => b.timestamp.compareTo(a.timestamp)),
				hasMore: results.length >= _pageSize,
			);
		} catch (e) {
			state = state.copyWith(
				loadingMore: false,
				error: e.toString(),
			);
		}
	}

	Future<void> markAsRead(String notificationId) async {
		final index = state.items.indexWhere((e) => e.id == notificationId);
		if (index < 0) {
			return;
		}

		final selected = state.items[index];
		if (selected.isRead) {
			return;
		}

		final updated = List<AppNotification>.from(state.items);
		updated[index] = AppNotification(
			id: selected.id,
			type: selected.type,
			title: selected.title,
			message: selected.message,
			timestamp: selected.timestamp,
			isRead: true,
			data: selected.data,
		);

		state = state.copyWith(
			items: updated,
			unreadCount: (state.unreadCount - 1).clamp(0, 1 << 31),
		);

		try {
			await _repository.markAsRead(notificationId: notificationId);
		} catch (_) {
			// Keep optimistic UI state.
		}
	}

	Future<void> markAllAsRead() async {
		if (state.items.isEmpty || state.unreadCount == 0) {
			return;
		}

		final userId = await _resolveUserId();
		if (userId == null) {
			return;
		}

		final updated = state.items
				.map(
					(e) => e.isRead
							? e
							: AppNotification(
									id: e.id,
									type: e.type,
									title: e.title,
									message: e.message,
									timestamp: e.timestamp,
									isRead: true,
									data: e.data,
								),
				)
				.toList();

		state = state.copyWith(items: updated, unreadCount: 0);

		try {
			await _repository.markAllAsRead(userId: userId);
		} catch (_) {
			// Keep optimistic UI state.
		}
	}

	void setFilter(NotificationFilter filter) {
		if (filter == state.filter) {
			return;
		}
		state = state.copyWith(filter: filter);
	}
}

final notificationsProvider = NotifierProvider<NotificationsController, NotificationsState>(
	NotificationsController.new,
);
