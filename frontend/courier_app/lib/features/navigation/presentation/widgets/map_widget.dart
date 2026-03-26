import 'dart:math' as math;
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';

import '../../../../config/runtime_config.dart';
import '../../../../core/theme/app_colors.dart';
import '../models/live_tracking_position.dart';

class CourierTrackingMapWidget extends StatefulWidget {
	final MapController mapController;
	final LiveTrackingPosition? position;
	final String? vehicleType;
	final VoidCallback? onUserInteracted;
	final VoidCallback? onMapReady;

	const CourierTrackingMapWidget({
		super.key,
		required this.mapController,
		required this.position,
		this.vehicleType,
		this.onUserInteracted,
		this.onMapReady,
	});

	@override
	State<CourierTrackingMapWidget> createState() => _CourierTrackingMapWidgetState();
}

class _CourierTrackingMapWidgetState extends State<CourierTrackingMapWidget>
		with SingleTickerProviderStateMixin {
	late final AnimationController _animationController;
	LatLng? _animatedPoint;
	LatLng? _animationStart;
	LatLng? _animationEnd;

	@override
	void initState() {
		super.initState();
		_animatedPoint = _targetPoint;
		_animationController = AnimationController(
			vsync: this,
			duration: const Duration(milliseconds: 900),
		)
			..addListener(_tickAnimation);
	}

	@override
	void didUpdateWidget(covariant CourierTrackingMapWidget oldWidget) {
		super.didUpdateWidget(oldWidget);

		final target = _targetPoint;
		if (target == null) {
			return;
		}

		if (_animatedPoint == null) {
			setState(() {
				_animatedPoint = target;
			});
			return;
		}

		final moved = _animatedPoint!.latitude != target.latitude ||
				_animatedPoint!.longitude != target.longitude;

		if (moved) {
			_animationStart = _animatedPoint;
			_animationEnd = target;
			_animationController.forward(from: 0);
		}
	}

	@override
	void dispose() {
		_animationController
			..removeListener(_tickAnimation)
			..dispose();
		super.dispose();
	}

	LatLng? get _targetPoint {
		final p = widget.position;
		if (p == null) {
			return null;
		}
		return LatLng(p.latitude, p.longitude);
	}

	void _tickAnimation() {
		final start = _animationStart;
		final end = _animationEnd;
		if (start == null || end == null) {
			return;
		}

		final t = Curves.easeOutCubic.transform(_animationController.value);
		setState(() {
			_animatedPoint = LatLng(
				lerpDouble(start.latitude, end.latitude, t) ?? end.latitude,
				lerpDouble(start.longitude, end.longitude, t) ?? end.longitude,
			);
		});
	}

	@override
	Widget build(BuildContext context) {
		final hasToken = RuntimeConfig.mapboxAccessToken.trim().isNotEmpty;
		if (!hasToken) {
			return Container(
				color: Colors.grey.shade200,
				alignment: Alignment.center,
				padding: const EdgeInsets.all(24),
				child: const Text(
					'Mapbox token manquant. Ajoutez mapboxAccessToken ou mapboxToken dans assets/config/config.json.',
					textAlign: TextAlign.center,
				),
			);
		}

		final initialCenter = _animatedPoint ?? const LatLng(33.5731, -7.5898);

		return FlutterMap(
			mapController: widget.mapController,
			options: MapOptions(
				initialCenter: initialCenter,
				initialZoom: 16,
				onMapReady: widget.onMapReady,
				onPositionChanged: (camera, hasGesture) {
					if (hasGesture) {
						widget.onUserInteracted?.call();
					}
				},
			),
			children: [
				TileLayer(
					urlTemplate:
							'${_styleTilesBaseUrl(RuntimeConfig.mapboxStyleUri)}/tiles/512/{z}/{x}/{y}@2x?access_token=${RuntimeConfig.mapboxAccessToken}',
					userAgentPackageName: 'com.speedline.courier_app',
					maxZoom: 22,
				),
				if (_animatedPoint != null && widget.position != null)
					CircleLayer(
						circles: [
							CircleMarker(
								point: _animatedPoint!,
								useRadiusInMeter: true,
								radius: widget.position!.accuracyMeters.clamp(2, 120).toDouble(),
								color: widget.position!.precisionColor.withValues(alpha: 0.20),
								borderColor: widget.position!.precisionColor.withValues(alpha: 0.65),
								borderStrokeWidth: 1.5,
							),
						],
					),
				if (_animatedPoint != null && widget.position != null)
					MarkerLayer(
						markers: [
							Marker(
								point: _animatedPoint!,
								width: 72,
								height: 72,
								child: Transform.rotate(
									angle: widget.position!.headingDegrees * math.pi / 180,
									child: Container(
										decoration: BoxDecoration(
											color: Colors.white,
											shape: BoxShape.circle,
											boxShadow: [
												BoxShadow(
																									color: Colors.black.withValues(alpha: 0.12),
													blurRadius: 10,
												),
											],
										),
										child: Icon(
											_vehicleIcon(widget.vehicleType),
											color: AppColors.primary,
											size: 34,
										),
									),
								),
							),
						],
					),
			],
		);
	}

	IconData _vehicleIcon(String? vehicleType) {
		final normalized = (vehicleType ?? '').toUpperCase();
		if (normalized.contains('MOTORCYCLE')) {
			return Icons.two_wheeler;
		}
		return Icons.pedal_bike;
	}

	String _styleTilesBaseUrl(String styleUri) {
		final prefix = 'mapbox://styles/';
		if (!styleUri.startsWith(prefix)) {
			return 'https://api.mapbox.com/styles/v1/mapbox/navigation-day-v1';
		}

		final parts = styleUri.replaceFirst(prefix, '').split('/');
		if (parts.length < 2 || parts[0].isEmpty || parts[1].isEmpty) {
			return 'https://api.mapbox.com/styles/v1/mapbox/navigation-day-v1';
		}
		return 'https://api.mapbox.com/styles/v1/${parts[0]}/${parts[1]}';
	}
}
