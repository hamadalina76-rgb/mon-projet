import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:latlong2/latlong.dart';

import '../../../../features/navigation/presentation/models/live_tracking_position.dart';
import '../../../../features/navigation/presentation/widgets/map_widget.dart';
import '../../../../features/auth/domain/entities/courier.dart';
import '../../../../providers/location_websocket_provider.dart';
import '../../../../providers/tracking_provider.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../providers/current_courier_provider.dart';
import '../../../../config/runtime_config.dart';

class CourierHomeScreen extends ConsumerStatefulWidget {
  const CourierHomeScreen({super.key, this.canAccessApp = true});

  final bool canAccessApp;

  @override
  ConsumerState<CourierHomeScreen> createState() => _CourierHomeScreenState();
}

class _CourierHomeScreenState extends ConsumerState<CourierHomeScreen> {
  final MapController _mapController = MapController();
  final Dio _nominatim = Dio(
    BaseOptions(
      baseUrl: 'https://nominatim.openstreetmap.org',
      connectTimeout: const Duration(seconds: 5),
      receiveTimeout: const Duration(seconds: 5),
      headers: const {
        'User-Agent': 'SpeedLineCourierApp/1.0 (support@speedline.app)',
      },
    ),
  );

  LiveTrackingPosition? _currentPosition;
  LatLng? _lastGeocodedPoint;
  Timer? _geocodeDebounce;
  DateTime? _lastGeocodeAt;
  String _locationSubtitle = '';
  bool _followCourier = true;
  bool _mapReady = false;
  Timer? _activeDeliveryTicker;
  StreamSubscription<dynamic>? _positionSubscription;
  ProviderSubscription<TrackingState>? _trackingSubscription;

  void _startActiveDeliveryTicker() {
    _activeDeliveryTicker?.cancel();
    _activeDeliveryTicker = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) {
        return;
      }
      setState(() {});
    });
  }

  void _stopActiveDeliveryTicker() {
    _activeDeliveryTicker?.cancel();
    _activeDeliveryTicker = null;
  }

  Future<void> _playActiveDeliveryCue({required bool playSound}) async {
    await HapticFeedback.lightImpact();
    if (playSound) {
      await SystemSound.play(SystemSoundType.alert);
    }
  }

  bool _isSoundEnabledForCourier(Courier? courier) {
    return courier?.activeDeliverySoundEnabled ?? false;
  }

  String _formatActiveDuration(DateTime? startedAt) {
    if (startedAt == null) {
      return '00:00';
    }
    final elapsed = DateTime.now().difference(startedAt);
    final hours = elapsed.inHours;
    final minutes = elapsed.inMinutes.remainder(60);
    final seconds = elapsed.inSeconds.remainder(60);
    if (hours > 0) {
      return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
    }
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  Future<bool> _confirmToggle(bool toOnline) async {
    final l10n = AppLocalizations.of(context)!;

    final result = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: Text(toOnline
              ? l10n.translate('go_online_question')
              : l10n.translate('go_offline_question')),
          content: Text(
            toOnline
                ? l10n.translate('go_online_desc')
                : l10n.translate('go_offline_desc'),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: Text(l10n.translate('cancel')),
            ),
            FilledButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: Text(l10n.translate('confirm')),
            ),
          ],
        );
      },
    );
    return result ?? false;
  }

  void _zoomBy(double delta) {
    try {
      final camera = _mapController.camera;
      final nextZoom = (camera.zoom + delta).clamp(3.0, 19.0);
      _mapController.move(camera.center, nextZoom);
    } catch (_) {
      // Ignore zoom command while map is not ready.
    }
  }

  @override
  void initState() {
    super.initState();
    final initialPayload =
        ref.read(locationWebSocketProvider.notifier).lastPublishedPosition;
    _currentPosition = initialPayload == null
        ? null
        : LiveTrackingPosition.fromPayload(initialPayload);

    _positionSubscription = ref.read(locationWebSocketProvider.notifier).positionStream.listen((payload) {
      final next = LiveTrackingPosition.fromPayload(payload);
      if (!mounted || next == null) {
        return;
      }
      setState(() {
        _currentPosition = next;
      });
      _scheduleReverseGeocode(next);
      if (_followCourier && _mapReady) {
        _centerOnCourier();
      }
    });

    if (_currentPosition != null) {
      _scheduleReverseGeocode(_currentPosition!);
    }

    _trackingSubscription = ref.listenManual<TrackingState>(trackingProvider, (previous, next) {
      final wasActive = previous?.hasActiveDelivery ?? false;
      if (!wasActive && next.hasActiveDelivery) {
        _startActiveDeliveryTicker();
        final courierState = ref.read(currentCourierProvider);
        final courier = courierState is AsyncData<Courier?> ? courierState.value : null;
        unawaited(
          _playActiveDeliveryCue(playSound: _isSoundEnabledForCourier(courier)),
        );
      }
      if (wasActive && !next.hasActiveDelivery) {
        _stopActiveDeliveryTicker();
      }
    });
  }

  @override
  void dispose() {
    _geocodeDebounce?.cancel();
    _stopActiveDeliveryTicker();
    _positionSubscription?.cancel();
    _trackingSubscription?.close();
    super.dispose();
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

  String? _resolveProfileImageUrl(String? rawUrl) {
    if (rawUrl == null || rawUrl.isEmpty) {
      return null;
    }
    if (rawUrl.startsWith('http://') || rawUrl.startsWith('https://')) {
      return rawUrl;
    }
    return '${RuntimeConfig.apiBaseUrl}$rawUrl';
  }

  void _scheduleReverseGeocode(LiveTrackingPosition position) {
    final now = DateTime.now();
    if (_lastGeocodeAt != null && now.difference(_lastGeocodeAt!).inMilliseconds < 1200) {
      return;
    }

    final nextPoint = LatLng(position.latitude, position.longitude);
    if (_lastGeocodedPoint != null) {
      final movedMeters = const Distance().as(
        LengthUnit.Meter,
        _lastGeocodedPoint!,
        nextPoint,
      );
      if (movedMeters < 30) {
        return;
      }
    }

    _geocodeDebounce?.cancel();
    _geocodeDebounce = Timer(const Duration(milliseconds: 1200), () async {
      try {
        final response = await _nominatim.get<Map<String, dynamic>>(
          '/reverse',
          queryParameters: {
            'format': 'jsonv2',
            'lat': position.latitude.toStringAsFixed(6),
            'lon': position.longitude.toStringAsFixed(6),
            'accept-language': Localizations.localeOf(context).languageCode,
            'addressdetails': 1,
            'zoom': 18,
          },
        );

        final data = response.data;
        if (data == null || !mounted) {
          return;
        }

        final address = data['address'];
        String? road;
        String? city;
        if (address is Map) {
          road = (address['road'] ?? address['pedestrian'] ?? address['path'] ?? address['footway'])?.toString();
          city = (address['city'] ?? address['town'] ?? address['village'] ?? address['state'])?.toString();
        }

        final label = [road, city].where((e) => e != null && e.isNotEmpty).join(', ');
        if (label.isNotEmpty) {
          setState(() {
            _locationSubtitle = label;
            _lastGeocodedPoint = nextPoint;
            _lastGeocodeAt = DateTime.now();
          });
        }
      } catch (_) {
        // Keep the last known readable label if Nominatim is temporarily unavailable.
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final trackingState = ref.watch(trackingProvider);
    final wsState = ref.watch(locationWebSocketProvider);
    final trackingController = ref.read(trackingProvider.notifier);
    final courierAsync = ref.watch(currentCourierProvider);
    final courier = courierAsync is AsyncData ? courierAsync.value : null;
    final profileUrl = _resolveProfileImageUrl(courier?.photoUrl);

    final statusText = _statusLabel(trackingState, _currentPosition, l10n);
    final wsBadgeText = _wsLabel(trackingState, wsState, l10n);
    final wsBadgeColor = _wsColor(trackingState, wsState);

    return Scaffold(
      backgroundColor: Colors.white,
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
            right: 14.w,
            bottom: 290.h,
            child: Column(
              children: [
                if (!_followCourier)
                  Padding(
                    padding: EdgeInsets.only(bottom: 10.h),
                    child: _MapActionButton(
                      icon: Icons.my_location,
                      onTap: _currentPosition == null
                          ? null
                          : () {
                              setState(() {
                                _followCourier = true;
                              });
                              _centerOnCourier();
                            },
                    ),
                  ),
                _MapActionButton(
                  icon: Icons.add,
                  onTap: () => _zoomBy(1),
                ),
                SizedBox(height: 10.h),
                _MapActionButton(
                  icon: Icons.remove,
                  onTap: () => _zoomBy(-1),
                ),
              ],
            ),
          ),

          Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: Container(
              padding: EdgeInsets.only(
                top: MediaQuery.of(context).padding.top + 16.h,
                left: 16.w,
                right: 16.w,
                bottom: 16.h,
              ),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.black.withValues(alpha: 0.6),
                    Colors.transparent,
                  ],
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Row(
                      children: [
                        CircleAvatar(
                          radius: 24.r,
                          backgroundColor: Colors.white,
                          backgroundImage: profileUrl != null
                              ? NetworkImage(profileUrl)
                              : null,
                          child: (courier?.photoUrl == null)
                              ? Icon(Icons.person, color: Colors.grey[400])
                              : null,
                        ),
                        SizedBox(width: 12.w),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '${l10n.translate('hello')}, ${courier?.firstName ?? l10n.translate('courier')}',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  color: Colors.white,
                                  fontSize: 18.sp,
                                  fontWeight: FontWeight.bold,
                                  shadows: const [Shadow(color: Colors.black45, blurRadius: 4)],
                                ),
                              ),
                              Text(
                                _locationSubtitle.isEmpty
                                    ? l10n.translate('home_ready_to_deliver')
                                    : _locationSubtitle,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.9),
                                  fontSize: 14.sp,
                                  shadows: const [Shadow(color: Colors.black45, blurRadius: 4)],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(width: 10.w),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        padding:
                            EdgeInsets.symmetric(horizontal: 10.w, vertical: 6.h),
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
                      SizedBox(width: 10.w),
                      CircleAvatar(
                        radius: 20.r,
                        backgroundColor: Colors.white,
                        child: Icon(Icons.notifications, color: AppColors.primary, size: 20.sp),
                      ),
                    ],
                  )
                ],
              ),
            ),
          ),

          AnimatedPositioned(
            duration: const Duration(milliseconds: 280),
            curve: Curves.easeOutCubic,
            top: trackingState.isOnline && trackingState.hasActiveDelivery
                ? MediaQuery.of(context).padding.top + 92.h
                : MediaQuery.of(context).padding.top + 66.h,
            left: 16.w,
            right: 16.w,
            child: IgnorePointer(
              ignoring: !(trackingState.isOnline && trackingState.hasActiveDelivery),
              child: AnimatedOpacity(
                duration: const Duration(milliseconds: 220),
                curve: Curves.easeOut,
                opacity: trackingState.isOnline && trackingState.hasActiveDelivery ? 1 : 0,
                child: GestureDetector(
                  onTap: () => context.push('/active-delivery'),
                  child: Container(
                    padding: EdgeInsets.symmetric(horizontal: 14.w, vertical: 12.h),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFD166),
                      borderRadius: BorderRadius.circular(14.r),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.12),
                          blurRadius: 12,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.delivery_dining, color: Colors.black87, size: 22.sp),
                        SizedBox(width: 10.w),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                l10n.translate('active_delivery_in_progress'),
                                style: TextStyle(
                                  fontSize: 14.sp,
                                  fontWeight: FontWeight.w800,
                                  color: Colors.black87,
                                ),
                              ),
                              Text(
                                '${l10n.translate('duration')}: ${_formatActiveDuration(trackingState.activeDeliveryStartedAt)}',
                                style: TextStyle(
                                  fontSize: 12.sp,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.black87.withValues(alpha: 0.8),
                                ),
                              ),
                            ],
                          ),
                        ),
                        Container(
                          padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 6.h),
                          decoration: BoxDecoration(
                            color: Colors.black87,
                            borderRadius: BorderRadius.circular(999),
                          ),
                          child: Text(
                            l10n.translate('view'),
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 12.sp,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),

          // 3. Status Bar & Offline/Online Toggle Bottom Sheet
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: _buildBottomPanel(
              context,
              trackingState: trackingState,
              trackingController: trackingController,
              statusText: statusText,
              position: _currentPosition,
              l10n: l10n,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomPanel(
    BuildContext context, {
    required TrackingState trackingState,
    required TrackingController trackingController,
    required String statusText,
    required LiveTrackingPosition? position,
    required AppLocalizations l10n,
  }) {
    final bool isOnline = trackingState.isOnline;
    final bool isBusy = trackingState.isBusy;
    final bool offlineBlockedByDelivery = isOnline && trackingState.hasActiveDelivery;
    final bool canToggle = widget.canAccessApp && !isBusy && !offlineBlockedByDelivery;
    final precisionText = position == null
      ? l10n.translate('waiting_gps_signal')
      : '${position.precisionLabel} (${position.accuracyMeters.toStringAsFixed(1)}m)';
    final precisionColor = position?.precisionColor ?? Colors.grey;
    final speedText = position?.formattedSpeed ?? '-- km/h';
    final batteryText = trackingState.batteryLevel == null
      ? '--%'
      : '${trackingState.batteryLevel}%';
    final shiftText = trackingState.shiftStartedAt == null
      ? '--:--'
      : DateFormat('HH:mm').format(trackingState.shiftStartedAt!);

    return Container(
      padding: EdgeInsets.symmetric(horizontal: 20.w, vertical: 14.h),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(24.r),
          topRight: Radius.circular(24.r),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.1),
            blurRadius: 20,
            offset: const Offset(0, -5),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  width: 12.w,
                  height: 12.w,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: isOnline ? Colors.green : Colors.grey,
                  ),
                ),
                SizedBox(width: 10.w),
                Text(
                  statusText,
                  style: TextStyle(
                    fontSize: 16.sp,
                    fontWeight: FontWeight.w600,
                    color: isOnline ? Colors.green[700] : Colors.grey[700],
                  ),
                ),
              ],
            ),
            SizedBox(height: 10.h),
            Container(
              width: double.infinity,
              padding: EdgeInsets.symmetric(horizontal: 10.w, vertical: 7.h),
              decoration: BoxDecoration(
                color: precisionColor.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(999),
              ),
              child: Text(
                'Precision GPS: $precisionText',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: precisionColor,
                  fontWeight: FontWeight.w700,
                  fontSize: 12.sp,
                ),
              ),
            ),
            SizedBox(height: 10.h),
            Row(
              children: [
                Expanded(child: _buildMiniStat(l10n.translate('speed'), speedText)),
                Expanded(child: _buildMiniStat(l10n.translate('battery'), batteryText)),
                Expanded(child: _buildMiniStat(l10n.translate('shift_start'), shiftText)),
              ],
            ),
            SizedBox(height: 12.h),
            if (!widget.canAccessApp)
              Container(
                margin: EdgeInsets.only(bottom: 16.h),
                padding: EdgeInsets.all(12.w),
                decoration: BoxDecoration(
                  color: Colors.orange.shade50,
                  borderRadius: BorderRadius.circular(12.r),
                  border: Border.all(color: Colors.orange.shade200),
                ),
                child: Row(
                  children: [
                    Icon(Icons.info_outline, color: Colors.orange.shade700, size: 24.sp),
                    SizedBox(width: 12.w),
                    Expanded(
                      child: Text(
                        l10n.translate('account_under_review_profile_only'),
                        style: TextStyle(fontSize: 12.sp, color: Colors.orange.shade900),
                      ),
                    ),
                  ],
                ),
              ),
            if (offlineBlockedByDelivery)
              Container(
                width: double.infinity,
                margin: EdgeInsets.only(bottom: 12.h),
                padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 10.h),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF2CC),
                  borderRadius: BorderRadius.circular(12.r),
                  border: Border.all(color: const Color(0xFFFFD166)),
                ),
                child: Row(
                  children: [
                    Icon(Icons.lock_clock, color: Colors.orange.shade800, size: 20.sp),
                    SizedBox(width: 8.w),
                    Expanded(
                      child: Text(
                        l10n.translate('finish_delivery_before_offline'),
                        style: TextStyle(
                          fontSize: 12.sp,
                          fontWeight: FontWeight.w600,
                          color: Colors.orange.shade900,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            GestureDetector(
              onTap: !canToggle
                  ? null
                  : () async {
                      final targetOnline = !isOnline;
                      final confirmed = await _confirmToggle(targetOnline);
                      if (!confirmed || !context.mounted) {
                        return;
                      }

                      await trackingController.setOnline(
                        context,
                        targetOnline,
                        inDelivery: false,
                      );
                      final message = ref.read(trackingProvider).blockingMessage;
                      if (message != null && context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(message)),
                        );
                      }
                    },
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 300),
                width: double.infinity,
                height: 56.h,
                decoration: BoxDecoration(
                  color: isBusy
                      ? Colors.grey[300]
                      : (offlineBlockedByDelivery
                          ? Colors.orange.shade600
                          : (isOnline ? Colors.redAccent : AppColors.primary)),
                  borderRadius: BorderRadius.circular(30.r),
                  boxShadow: [
                    if (!isBusy && !offlineBlockedByDelivery)
                      BoxShadow(
                        color: (isOnline ? Colors.redAccent : AppColors.primary).withValues(alpha: 0.3),
                        blurRadius: 10,
                        offset: const Offset(0, 4),
                      ),
                  ],
                ),
                child: Center(
                  child: isBusy
                      ? SizedBox(
                          width: 24.w,
                          height: 24.w,
                          child: const CircularProgressIndicator(
                            strokeWidth: 3,
                            valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : Text(
                          offlineBlockedByDelivery
                            ? l10n.translate('delivery_in_progress_upper')
                            : (isOnline
                              ? l10n.translate('go_offline_upper')
                              : l10n.translate('go_online_upper')),
                          style: TextStyle(
                            fontSize: 16.sp,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                            letterSpacing: 1.2,
                          ),
                        ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _statusLabel(TrackingState state, LiveTrackingPosition? position, AppLocalizations l10n) {
    if (!state.isOnline) {
      return l10n.translate('offline');
    }
    if (state.hasActiveDelivery) {
      return l10n.translate('in_delivery');
    }
    if (position == null) {
      return l10n.translate('online_waiting_gps');
    }
    if (position.speedKmh > 3) {
      return l10n.translate('in_delivery');
    }
    return l10n.translate('online_waiting');
  }

  String _wsLabel(TrackingState state, LocationWebSocketState wsState, AppLocalizations l10n) {
    if (!state.isOnline) {
      return l10n.translate('off');
    }
    if (wsState.status == LocationWebSocketStatus.connected) {
      return l10n.translate('connected');
    }
    return l10n.translate('offline');
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

  Widget _buildMiniStat(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(fontSize: 12.sp, color: Colors.grey[500]),
        ),
        Text(
          value,
          style: TextStyle(fontSize: 14.sp, fontWeight: FontWeight.w600, color: Colors.black87),
        ),
      ],
    );
  }

}

class _MapActionButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback? onTap;

  const _MapActionButton({
    required this.icon,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(14.r),
      elevation: 3,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14.r),
        child: SizedBox(
          width: 44.w,
          height: 44.w,
          child: Icon(icon, color: Colors.black87),
        ),
      ),
    );
  }
}
