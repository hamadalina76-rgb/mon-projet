import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:intl/intl.dart';
import 'package:latlong2/latlong.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../features/navigation/presentation/models/live_tracking_position.dart';
import '../../../../features/navigation/presentation/widgets/map_widget.dart';
import '../../../../providers/current_courier_provider.dart';
import '../../../../providers/location_websocket_provider.dart';
import '../../../../providers/tracking_provider.dart';

class ActiveDeliveryScreen extends ConsumerStatefulWidget {
	const ActiveDeliveryScreen({super.key});

	@override
	ConsumerState<ActiveDeliveryScreen> createState() => _ActiveDeliveryScreenState();
}

class _ActiveDeliveryScreenState extends ConsumerState<ActiveDeliveryScreen> {
	final MapController _mapController = MapController();
	StreamSubscription<Map<String, dynamic>>? _positionSub;

	LiveTrackingPosition? _currentPosition;
	bool _followCourier = true;
	bool _mapReady = false;

	@override
	void initState() {
		super.initState();
		final initialPayload = ref.read(locationWebSocketProvider.notifier).lastPublishedPosition;
		_currentPosition = initialPayload == null
				? null
				: LiveTrackingPosition.fromPayload(initialPayload);
		_positionSub = ref
				.read(locationWebSocketProvider.notifier)
				.positionStream
				.listen(_handleIncomingPayload);
	}

	@override
	void dispose() {
		_positionSub?.cancel();
		super.dispose();
	}

	void _handleIncomingPayload(Map<String, dynamic> payload) {
		final next = LiveTrackingPosition.fromPayload(payload);
		if (next == null || !mounted) {
			return;
		}

		setState(() {
			_currentPosition = next;
		});

		if (_followCourier && _mapReady) {
			_centerOnCourier();
		}
	}

	void _centerOnCourier() {
		final p = _currentPosition;
		if (p == null) {
			return;
		}

		_mapController.move(
			LatLng(p.latitude, p.longitude),
			_mapController.camera.zoom,
		);
	}

	@override
	Widget build(BuildContext context) {
		final trackingState = ref.watch(trackingProvider);
		final wsState = ref.watch(locationWebSocketProvider);
		final courierAsync = ref.watch(currentCourierProvider);
		final courier = courierAsync is AsyncData ? courierAsync.value : null;

		final statusText = _statusLabel(trackingState, _currentPosition);
		final wsBadgeText = _wsLabel(trackingState, wsState);
		final wsBadgeColor = _wsColor(trackingState, wsState);

		return Scaffold(
			body: Stack(
				children: [
					Positioned.fill(
						child: CourierTrackingMapWidget(
							mapController: _mapController,
							position: _currentPosition,
							vehicleType: courier?.vehicleType,
							onMapReady: () {
								_mapReady = true;
								if (_followCourier) {
									_centerOnCourier();
								}
							},
							onUserInteracted: () {
								if (_followCourier) {
									setState(() {
										_followCourier = false;
									});
								}
							},
						),
					),
					Positioned(
						top: 46.h,
						left: 14.w,
						right: 14.w,
						child: SafeArea(
							child: Column(
								children: [
									_TopBanner(
										statusText: statusText,
										wsBadgeText: wsBadgeText,
										wsBadgeColor: wsBadgeColor,
									),
									if (!_followCourier)
										Padding(
											padding: EdgeInsets.only(top: 8.h),
											child: Align(
												alignment: Alignment.centerRight,
												child: ElevatedButton.icon(
													style: ElevatedButton.styleFrom(
														backgroundColor: Colors.white,
														foregroundColor: AppColors.primary,
													),
													onPressed: _currentPosition == null
															? null
															: () {
																	setState(() {
																		_followCourier = true;
																	});
																	_centerOnCourier();
																},
													icon: const Icon(Icons.my_location),
													label: const Text('Recentrer'),
												),
											),
										),
								],
							),
						),
					),
					Positioned(
						left: 12.w,
						right: 12.w,
						bottom: 22.h,
						child: SafeArea(
							child: _BottomHud(
								position: _currentPosition,
								shiftStartedAt: trackingState.shiftStartedAt,
								batteryLevel:
										trackingState.batteryLevel ?? _currentPosition?.batteryLevel,
							),
						),
					),
					Positioned(
						top: 42.h,
						left: 8.w,
						child: SafeArea(
							child: IconButton.filled(
								style: IconButton.styleFrom(backgroundColor: Colors.white),
								onPressed: () => Navigator.of(context).pop(),
								icon: const Icon(Icons.arrow_back, color: Colors.black87),
							),
						),
					),
				],
			),
		);
	}

	String _statusLabel(TrackingState state, LiveTrackingPosition? position) {
		if (!state.isOnline) {
			return 'Hors ligne';
		}
		if (position == null) {
			return 'En ligne - En attente GPS';
		}
		if (position.speedKmh > 3) {
			return 'En livraison';
		}
		return 'En ligne - En attente';
	}

	String _wsLabel(TrackingState state, LocationWebSocketState wsState) {
		if (!state.isOnline) {
			return 'Off';
		}
		if (wsState.status == LocationWebSocketStatus.connected) {
			return 'Connecte';
		}
		return 'Hors ligne';
	}

	Color _wsColor(TrackingState state, LocationWebSocketState wsState) {
		if (!state.isOnline) {
			return Colors.grey;
		}
		if (wsState.status == LocationWebSocketStatus.connected) {
			return AppColors.success;
		}
		return AppColors.error;
	}
}

class _TopBanner extends StatelessWidget {
	final String statusText;
	final String wsBadgeText;
	final Color wsBadgeColor;

	const _TopBanner({
		required this.statusText,
		required this.wsBadgeText,
		required this.wsBadgeColor,
	});

	@override
	Widget build(BuildContext context) {
		return Container(
			width: double.infinity,
			padding: EdgeInsets.symmetric(horizontal: 14.w, vertical: 12.h),
			decoration: BoxDecoration(
				color: Colors.white.withValues(alpha: 0.94),
				borderRadius: BorderRadius.circular(16.r),
				boxShadow: [
					BoxShadow(
						color: Colors.black.withValues(alpha: 0.08),
						blurRadius: 12,
					),
				],
			),
			child: Row(
				children: [
					Expanded(
						child: Text(
							statusText,
							style: TextStyle(
								fontSize: 15.sp,
								fontWeight: FontWeight.w700,
								color: Colors.black87,
							),
						),
					),
					Container(
						padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 6.h),
						decoration: BoxDecoration(
							color: wsBadgeColor.withValues(alpha: 0.12),
							borderRadius: BorderRadius.circular(999),
						),
						child: Row(
							children: [
								Container(
									width: 8.w,
									height: 8.w,
									decoration: BoxDecoration(
										color: wsBadgeColor,
										shape: BoxShape.circle,
									),
								),
								SizedBox(width: 6.w),
								Text(
									wsBadgeText,
									style: TextStyle(
										fontSize: 12.sp,
										color: wsBadgeColor,
										fontWeight: FontWeight.w700,
									),
								),
							],
						),
					),
				],
			),
		);
	}
}

class _BottomHud extends StatelessWidget {
	final LiveTrackingPosition? position;
	final DateTime? shiftStartedAt;
	final int? batteryLevel;

	const _BottomHud({
		required this.position,
		required this.shiftStartedAt,
		required this.batteryLevel,
	});

	@override
	Widget build(BuildContext context) {
		final precisionText = position == null
			? 'En attente du signal GPS'
				: '${position!.precisionLabel} (${position!.accuracyMeters.toStringAsFixed(1)}m)';

		final precisionColor = position?.precisionColor ?? Colors.grey;
		final speedText = position?.formattedSpeed ?? '-- km/h';
		final batteryText = batteryLevel == null ? '--%' : '$batteryLevel%';
		final shiftText = shiftStartedAt == null
				? '--:--'
				: DateFormat('HH:mm').format(shiftStartedAt!);

		return Container(
			padding: EdgeInsets.symmetric(horizontal: 14.w, vertical: 12.h),
			decoration: BoxDecoration(
				color: Colors.white.withValues(alpha: 0.96),
				borderRadius: BorderRadius.circular(16.r),
				boxShadow: [
					BoxShadow(
						color: Colors.black.withValues(alpha: 0.10),
						blurRadius: 12,
					),
				],
			),
			child: Column(
				crossAxisAlignment: CrossAxisAlignment.start,
				mainAxisSize: MainAxisSize.min,
				children: [
					Container(
						padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 6.h),
						decoration: BoxDecoration(
								color: precisionColor.withValues(alpha: 0.12),
							borderRadius: BorderRadius.circular(999),
						),
						child: Text(
							'Precision GPS: $precisionText',
							style: TextStyle(
								color: precisionColor,
								fontWeight: FontWeight.w700,
								fontSize: 12.sp,
							),
						),
					),
					SizedBox(height: 10.h),
					Row(
						mainAxisAlignment: MainAxisAlignment.spaceBetween,
						children: [
							_InfoCell(label: 'Vitesse', value: speedText),
							_InfoCell(label: 'Batterie', value: batteryText),
							_InfoCell(label: 'Debut service', value: shiftText),
						],
					),
				],
			),
		);
	}
}

class _InfoCell extends StatelessWidget {
	final String label;
	final String value;

	const _InfoCell({required this.label, required this.value});

	@override
	Widget build(BuildContext context) {
		return Expanded(
			child: Column(
				crossAxisAlignment: CrossAxisAlignment.start,
				children: [
					Text(
						label,
						style: TextStyle(
							color: Colors.grey[600],
							fontSize: 11.sp,
						),
					),
					SizedBox(height: 3.h),
					Text(
						value,
						style: TextStyle(
							color: Colors.black87,
							fontWeight: FontWeight.w700,
							fontSize: 13.sp,
						),
					),
				],
			),
		);
	}
}
