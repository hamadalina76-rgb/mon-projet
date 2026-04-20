import 'dart:async';
import 'dart:math' as math;
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/utils/delivery_zone_utils.dart';
import '../../../location/data/models/saved_location.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../../profile/data/models/address_model.dart';
import '../../../profile/presentation/providers/address_provider.dart';
import '../../cart_providers.dart';

class CheckoutScreen extends ConsumerStatefulWidget {
  const CheckoutScreen({super.key});

  @override
  ConsumerState<CheckoutScreen> createState() => _CheckoutScreenState();
}

enum _DeliveryMode { now, schedule }

enum _ScheduleClockMode { digital, analog }

enum _AnalogDialSelection { hour, minute }

class _CheckoutScreenState extends ConsumerState<CheckoutScreen> {
  static const String _paymentMethodCash = 'CASH';
  static const int _scheduleLeadMinutes = 15;

  String? _loadedPartnerId;
  PartnerCartInfo? _partnerInfo;
  bool _isLoadingPartner = false;

  double? _deliveryFee;
  double _freeDeliveryThreshold = 0;
  bool _isLoadingDeliveryFee = false;

  AddressModel? _selectedSavedAddress;
  bool _useMapAddress = false;

  _DeliveryMode _deliveryMode = _DeliveryMode.now;
  DateTime? _scheduledDateTime;

  late final TextEditingController _promoController;
  String? _appliedPromoCode;
  String? _promoMessage;
  bool _promoMessageIsError = false;

  bool _isPlacingOrder = false;
  bool _showAllItems = false;

  @override
  void initState() {
    super.initState();
    _promoController = TextEditingController();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadAddressesForCurrentCustomer();
    });
  }

  @override
  void dispose() {
    _promoController.dispose();
    super.dispose();
  }

  void _loadAddressesForCurrentCustomer() {
    ref
        .read(authNotifierProvider)
        .whenOrNull(
          authenticated: (user) {
            ref.read(addressNotifierProvider.notifier).fetchAddresses(user.id);
          },
        );
  }

  String _money(double value) {
    if ((value % 1).abs() < 0.0001) return '${value.toStringAsFixed(0)} DT';
    return '${value.toStringAsFixed(2)} DT';
  }

  void _ensurePartnerLoaded(String? partnerId) {
    if (_loadedPartnerId == partnerId) return;

    _loadedPartnerId = partnerId;
    _partnerInfo = null;
    _deliveryFee = null;
    _freeDeliveryThreshold = 0;
    _isLoadingPartner = false;
    _isLoadingDeliveryFee = false;
    _deliveryMode = _DeliveryMode.now;
    _scheduledDateTime = null;

    if (partnerId == null || partnerId.isEmpty) {
      if (mounted) setState(() {});
      return;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;
      setState(() {
        _isLoadingPartner = true;
        _isLoadingDeliveryFee = true;
      });

      final repo = ref.read(cartRepositoryProvider);
      final info = await repo.fetchPartnerInfo(partnerId);

      if (!mounted) return;
      setState(() {
        _partnerInfo = info;
        _isLoadingPartner = false;
        _deliveryFee = info?.deliveryFeeValue;
        _freeDeliveryThreshold = info?.freeDeliveryThreshold ?? 0;
        _isLoadingDeliveryFee = false;
        if (info != null && !info.isOpen) {
          _deliveryMode = _DeliveryMode.schedule;
        }
      });
    });
  }

  double _computeServiceFee(double subtotal) {
    final info = _partnerInfo;
    if (info == null) return 0;

    if (info.zoneServiceFee != null) {
      return info.zoneServiceFee!.clamp(0, double.infinity).toDouble();
    }

    final base = info.serviceFeeValue;
    if (base <= 0) return 0;

    if (info.serviceFeeIsPercentage) {
      final calculated = subtotal * (base / 100);
      return calculated.clamp(0, double.infinity).toDouble();
    }

    return base;
  }

  double _effectiveDeliveryFee(double subtotal) {
    if (_deliveryFee == null) return 0;
    if (_freeDeliveryThreshold > 0 && subtotal >= _freeDeliveryThreshold) {
      return 0;
    }
    return _deliveryFee!;
  }

  double _promoDiscount(double subtotal) {
    if ((_appliedPromoCode ?? '').toUpperCase() == 'TEST10') {
      return math.min(10, subtotal);
    }
    return 0;
  }

  String _addressSubtitle(AddressModel address) {
    final l10n = AppLocalizations.of(context);

    if ((address.formattedAddress ?? '').trim().isNotEmpty) {
      return address.formattedAddress!.trim();
    }

    final parts = <String>[
      if ((address.street ?? '').trim().isNotEmpty) address.street!.trim(),
      if ((address.city ?? '').trim().isNotEmpty) address.city!.trim(),
      if ((address.state ?? '').trim().isNotEmpty) address.state!.trim(),
    ];

    if (parts.isEmpty) {
      return l10n.translate('no_detailed_address');
    }

    return parts.join(', ');
  }

  AddressModel? _defaultAddress(List<AddressModel> addresses) {
    for (final address in addresses) {
      if (address.isDefault) return address;
    }
    return addresses.isNotEmpty ? addresses.first : null;
  }

  AddressModel? _effectiveSavedAddress(List<AddressModel> addresses) {
    if (_selectedSavedAddress == null) return _defaultAddress(addresses);

    for (final addr in addresses) {
      if (addr.id == _selectedSavedAddress!.id) {
        return addr;
      }
    }

    return _defaultAddress(addresses);
  }

  String _addressTypeLabel(AddressType type) {
    final l10n = AppLocalizations.of(context);

    switch (type) {
      case AddressType.home:
        return l10n.translate('address_type_home');
      case AddressType.work:
        return l10n.translate('address_type_work');
      case AddressType.apartment:
        return l10n.translate('address_type_apartment');
      case AddressType.other:
        return l10n.translate('address_type_other');
    }
  }

  IconData _addressTypeIcon(AddressType type) {
    switch (type) {
      case AddressType.home:
        return Icons.home_rounded;
      case AddressType.work:
        return Icons.business_rounded;
      case AddressType.apartment:
        return Icons.apartment_rounded;
      case AddressType.other:
        return Icons.place_rounded;
    }
  }

  ({double? lat, double? lng}) _resolveCoordinates({
    required List<AddressModel> addresses,
    required SavedLocation? mapLocation,
    required AddressModel? effectiveSavedAddress,
  }) {
    if (_useMapAddress && mapLocation != null) {
      return (lat: mapLocation.latitude, lng: mapLocation.longitude);
    }

    if (effectiveSavedAddress?.latitude != null &&
        effectiveSavedAddress?.longitude != null) {
      return (
        lat: effectiveSavedAddress!.latitude,
        lng: effectiveSavedAddress.longitude,
      );
    }

    if (mapLocation != null) {
      return (lat: mapLocation.latitude, lng: mapLocation.longitude);
    }

    final fallback = _defaultAddress(addresses);
    if (fallback?.latitude != null && fallback?.longitude != null) {
      return (lat: fallback!.latitude, lng: fallback.longitude);
    }

    return (lat: null, lng: null);
  }

  Map<String, dynamic>? _buildDeliveryAddressPayload({
    required AddressModel? effectiveSavedAddress,
    required SavedLocation? mapLocation,
  }) {
    final bool useMapPayload = _useMapAddress || effectiveSavedAddress == null;

    if (useMapPayload && mapLocation != null) {
      final formatted = mapLocation.formattedAddress.trim();
      final customLabel = mapLocation.customLabel?.trim();
      return {
        'label': (customLabel == null || customLabel.isEmpty)
            ? AppLocalizations.of(context).translate('map_address_title')
            : customLabel,
        'deliveryAddress': formatted,
        'deliveryLocation': formatted,
        'street': mapLocation.street,
        'city': mapLocation.city,
        'state': mapLocation.state,
        'postalCode': mapLocation.postalCode,
        'country': mapLocation.country,
        'formattedAddress': formatted,
        'latitude': mapLocation.latitude,
        'longitude': mapLocation.longitude,
        'deliveryLatitude': mapLocation.latitude,
        'deliveryLongitude': mapLocation.longitude,
        'isFromMap': true,
      };
    }

    if (effectiveSavedAddress == null) {
      return null;
    }

    final subtitle = _addressSubtitle(effectiveSavedAddress);
    return {
      'addressId': effectiveSavedAddress.id.toString(),
      'label': effectiveSavedAddress.displayLabel,
      'deliveryAddress': subtitle,
      'deliveryLocation': subtitle,
      'street': effectiveSavedAddress.street,
      'building': effectiveSavedAddress.building,
      'floor': effectiveSavedAddress.floor,
      'apartment': effectiveSavedAddress.apartment,
      'city': effectiveSavedAddress.city,
      'postalCode': effectiveSavedAddress.postalCode,
      'state': effectiveSavedAddress.state,
      'country': effectiveSavedAddress.country,
      'formattedAddress': effectiveSavedAddress.formattedAddress ?? subtitle,
      'deliveryInstructions': effectiveSavedAddress.deliveryInstructions,
      'latitude': effectiveSavedAddress.latitude,
      'longitude': effectiveSavedAddress.longitude,
      'deliveryLatitude': effectiveSavedAddress.latitude,
      'deliveryLongitude': effectiveSavedAddress.longitude,
      'isFromMap': false,
    };
  }

  bool _isCurrentPartnerOutOfZone({double? userLat, double? userLng}) {
    final info = _partnerInfo;
    if (info == null) return false;

    return isOutsideDeliveryZone(
      deliveryRadius: info.deliveryRadius,
      distanceKm: info.distanceKm,
      userLat: userLat,
      userLng: userLng,
      partnerLat: info.latitude,
      partnerLng: info.longitude,
    );
  }

  String _formatScheduleDateTime(DateTime value) {
    final d = value.day.toString().padLeft(2, '0');
    final m = value.month.toString().padLeft(2, '0');
    final h = value.hour.toString().padLeft(2, '0');
    final min = value.minute.toString().padLeft(2, '0');
    return '$d/$m $h:$min';
  }

  void _applyPromo(double subtotal) {
    final l10n = AppLocalizations.of(context);
    final code = _promoController.text.trim().toUpperCase();

    setState(() {
      if (code.isEmpty) {
        _appliedPromoCode = null;
        _promoMessage = null;
        _promoMessageIsError = false;
        return;
      }

      if (code == 'TEST10') {
        _appliedPromoCode = code;
        _promoMessage =
            '${l10n.translate('promo_test_applied')}: -${_money(_promoDiscount(subtotal))}';
        _promoMessageIsError = false;
      } else {
        _appliedPromoCode = null;
        _promoMessage = l10n.translate('promo_invalid_test10');
        _promoMessageIsError = true;
      }
    });
  }

  Future<void> _pickSavedAddress(List<AddressModel> addresses) async {
    final l10n = AppLocalizations.of(context);

    if (addresses.isEmpty) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('no_saved_addresses'))),
      );
      return;
    }

    final chosen = await showModalBottomSheet<AddressModel>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) {
        return SafeArea(
          child: ListView.separated(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
            itemCount: addresses.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (_, index) {
              final address = addresses[index];
              return ListTile(
                leading: Icon(_addressTypeIcon(address.type)),
                title: Text(address.displayLabel),
                subtitle: Text(
                  _addressSubtitle(address),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                trailing: address.isDefault
                    ? Chip(label: Text(l10n.translate('default_label')))
                    : null,
                onTap: () => Navigator.of(sheetContext).pop(address),
              );
            },
          ),
        );
      },
    );

    if (chosen == null) return;

    setState(() {
      _selectedSavedAddress = chosen;
      _useMapAddress = false;
    });

    if (chosen.latitude != null && chosen.longitude != null) {
      await ref
          .read(locationNotifierProvider.notifier)
          .selectAddress(
            latitude: chosen.latitude!,
            longitude: chosen.longitude!,
            label: chosen.displayLabel,
            formattedAddress: chosen.formattedAddress ?? chosen.displayLabel,
            street: chosen.street ?? '',
            city: chosen.city ?? '',
            type: chosen.type,
          );
    }
  }

  Future<void> _pickAddressOnMap({
    required List<AddressModel> addresses,
    required AddressModel? effectiveSavedAddress,
    required SavedLocation? mapLocation,
  }) async {
    final coords = _resolveCoordinates(
      addresses: addresses,
      mapLocation: mapLocation,
      effectiveSavedAddress: effectiveSavedAddress,
    );

    await context.push(
      RouteNames.confirmLocation,
      extra: {
        'latitude': coords.lat ?? 36.8065,
        'longitude': coords.lng ?? 10.1815,
        'initialAddress':
            mapLocation?.formattedAddress ??
            (effectiveSavedAddress == null
                ? ''
                : _addressSubtitle(effectiveSavedAddress)),
      },
    );

    if (!mounted) return;
    setState(() {
      _useMapAddress = true;
    });
  }

  List<_PartnerTimeWindow> _windowsForDate(DateTime date) {
    final info = _partnerInfo;
    if (info == null || info.openingHours.isEmpty) {
      return const <_PartnerTimeWindow>[];
    }

    final dayName = _dayNameForDate(date);
    final previousDayName = _dayNameForDate(
      date.subtract(const Duration(days: 1)),
    );

    final windows = <_PartnerTimeWindow>[];

    for (final hour in info.openingHours) {
      if (hour.dayOfWeek.toUpperCase() != dayName || hour.isClosed) continue;

      if (hour.is24Hours) {
        windows.add(
          const _PartnerTimeWindow(startMinutes: 0, endMinutes: 1439),
        );
        continue;
      }

      final open = _parseTimeToMinutes(hour.openTime);
      final close = _parseTimeToMinutes(hour.closeTime);
      if (open == null || close == null) continue;

      if (close > open) {
        windows.add(_PartnerTimeWindow(startMinutes: open, endMinutes: close));
      } else {
        windows.add(_PartnerTimeWindow(startMinutes: open, endMinutes: 1439));
      }
    }

    for (final hour in info.openingHours) {
      if (hour.dayOfWeek.toUpperCase() != previousDayName ||
          hour.isClosed ||
          hour.is24Hours) {
        continue;
      }

      final open = _parseTimeToMinutes(hour.openTime);
      final close = _parseTimeToMinutes(hour.closeTime);
      if (open == null || close == null) continue;

      if (close <= open) {
        windows.add(_PartnerTimeWindow(startMinutes: 0, endMinutes: close));
      }
    }

    windows.sort((a, b) => a.startMinutes.compareTo(b.startMinutes));
    return _mergeOverlappingWindows(windows);
  }

  String _dayNameForDate(DateTime date) {
    const dayNames = <String>[
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];

    return dayNames[date.weekday - 1];
  }

  List<_PartnerTimeWindow> _mergeOverlappingWindows(
    List<_PartnerTimeWindow> windows,
  ) {
    if (windows.isEmpty) return const <_PartnerTimeWindow>[];

    final merged = <_PartnerTimeWindow>[];
    for (final window in windows) {
      if (merged.isEmpty) {
        merged.add(window);
        continue;
      }

      final last = merged.last;
      if (window.startMinutes <= last.endMinutes + 1) {
        merged[merged.length - 1] = _PartnerTimeWindow(
          startMinutes: last.startMinutes,
          endMinutes: window.endMinutes > last.endMinutes
              ? window.endMinutes
              : last.endMinutes,
        );
      } else {
        merged.add(window);
      }
    }

    return merged;
  }

  int? _parseTimeToMinutes(String? value) {
    if (value == null || value.trim().isEmpty) return null;

    final parts = value.trim().split(':');
    if (parts.length < 2) return null;

    final hour = int.tryParse(parts[0]);
    final minute = int.tryParse(parts[1]);
    if (hour == null || minute == null) return null;
    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;

    return (hour * 60) + minute;
  }

  DateTime _stripTime(DateTime value) {
    return DateTime(value.year, value.month, value.day);
  }

  bool _isSameDate(DateTime a, DateTime b) {
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  List<DateTime> _currentWeekDates() {
    final today = _stripTime(DateTime.now());

    return List<DateTime>.generate(
      7,
      (index) => today.add(Duration(days: index)),
    );
  }

  List<int> _buildAvailableMinutesForDate(DateTime date) {
    final target = _stripTime(date);
    final today = _stripTime(DateTime.now());
    if (target.isBefore(today)) {
      return const <int>[];
    }

    final windows = _windowsForDate(target);
    if (windows.isEmpty) {
      return const <int>[];
    }

    final now = DateTime.now();
    final minAllowed = _isSameDate(target, now)
        ? (now.hour * 60) + now.minute + _scheduleLeadMinutes
        : 0;

    final minutes = <int>[];
    final seen = <int>{};

    for (final window in windows) {
      var minute = window.startMinutes;
      if (minute < minAllowed) {
        minute = minAllowed;
      }
      if (minute > window.endMinutes) continue;

      for (var current = minute; current <= window.endMinutes; current++) {
        if (seen.add(current)) {
          minutes.add(current);
        }
      }
    }

    minutes.sort();
    return minutes;
  }

  List<int> _availableHoursFromMinutes(List<int> availableMinutes) {
    final hours = availableMinutes
        .map((minute) => minute ~/ 60)
        .toSet()
        .toList();
    hours.sort();
    return hours;
  }

  List<int> _availableMinutePartsForHour(List<int> availableMinutes, int hour) {
    final minutes = availableMinutes
        .where((minute) => (minute ~/ 60) == hour)
        .map((minute) => minute % 60)
        .toSet()
        .toList();
    minutes.sort();
    return minutes;
  }

  String _formatHourPart(int hour) {
    return hour.toString().padLeft(2, '0');
  }

  String _formatMinutePart(int minute) {
    return minute.toString().padLeft(2, '0');
  }

  String _formatScheduleDayLabel(DateTime date) {
    final l10n = AppLocalizations.of(context);
    final dayLabels = <String>[
      l10n.translate('day_short_monday'),
      l10n.translate('day_short_tuesday'),
      l10n.translate('day_short_wednesday'),
      l10n.translate('day_short_thursday'),
      l10n.translate('day_short_friday'),
      l10n.translate('day_short_saturday'),
      l10n.translate('day_short_sunday'),
    ];

    final d = date.day.toString().padLeft(2, '0');
    final m = date.month.toString().padLeft(2, '0');
    return '${dayLabels[date.weekday - 1]} $d/$m';
  }

  String _formatMinutesAsTime(int minute) {
    final h = (minute ~/ 60).toString().padLeft(2, '0');
    final m = (minute % 60).toString().padLeft(2, '0');
    return '$h:$m';
  }

  String _translateOrFallback(
    AppLocalizations l10n,
    String key,
    String fallback,
  ) {
    final translated = l10n.translate(key);
    return translated == key ? fallback : translated;
  }

  bool _isFrenchLocale(AppLocalizations l10n) {
    return l10n.locale.languageCode.toLowerCase().startsWith('fr');
  }

  List<DateTime> _openDatesInCurrentWeek() {
    return _currentWeekDates()
        .where((date) => _windowsForDate(date).isNotEmpty)
        .toList();
  }

  String _formatOpeningWindowsForDate(DateTime date, AppLocalizations l10n) {
    final windows = _windowsForDate(date);
    if (windows.isEmpty) {
      return _translateOrFallback(l10n, 'establishment_closed', 'Closed');
    }

    return windows
        .map(
          (window) =>
              '${_formatMinutesAsTime(window.startMinutes)} - ${_formatMinutesAsTime(window.endMinutes)}',
        )
        .join(' | ');
  }

  Future<DateTime?> _pickScheduledDateTime() async {
    final l10n = AppLocalizations.of(context);
    final info = _partnerInfo;
    if (info == null || info.openingHours.isEmpty) {
      if (!mounted) return null;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('failed_load_partner_hours')),
          backgroundColor: Colors.red,
        ),
      );
      return null;
    }

    final openDates = _openDatesInCurrentWeek();

    DateTime? initialDate;
    for (final date in openDates) {
      if (_buildAvailableMinutesForDate(date).isNotEmpty) {
        initialDate = date;
        break;
      }
    }

    if (initialDate == null) {
      if (!mounted) return null;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('no_slots_this_week')),
          backgroundColor: Colors.red,
        ),
      );
      return null;
    }

    var selectedDate = initialDate;
    var selectedMinute = _buildAvailableMinutesForDate(selectedDate).first;
    var clockMode = _ScheduleClockMode.analog;
    var analogSelection = _AnalogDialSelection.hour;

    final pickedDateTime = await showDialog<DateTime>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (dialogContext, setModalState) {
            final media = MediaQuery.of(dialogContext);
            final availableMinutes = _buildAvailableMinutesForDate(
              selectedDate,
            );
            if (!availableMinutes.contains(selectedMinute)) {
              selectedMinute = availableMinutes.isEmpty
                  ? -1
                  : availableMinutes.first;
            }

            final availableHours = _availableHoursFromMinutes(availableMinutes);
            var selectedHour = selectedMinute >= 0
                ? selectedMinute ~/ 60
                : (availableHours.isEmpty ? -1 : availableHours.first);
            if (availableHours.isNotEmpty &&
                !availableHours.contains(selectedHour)) {
              selectedHour = availableHours.first;
            }

            final availableMinuteParts = selectedHour >= 0
                ? _availableMinutePartsForHour(availableMinutes, selectedHour)
                : const <int>[];
            var selectedMinutePart = selectedMinute >= 0
                ? selectedMinute % 60
                : (availableMinuteParts.isEmpty
                      ? -1
                      : availableMinuteParts.first);
            if (availableMinuteParts.isNotEmpty &&
                !availableMinuteParts.contains(selectedMinutePart)) {
              selectedMinutePart = availableMinuteParts.first;
            }

            if (selectedHour >= 0 && selectedMinutePart >= 0) {
              selectedMinute = (selectedHour * 60) + selectedMinutePart;
            } else {
              selectedMinute = -1;
            }

            final isFrench = _isFrenchLocale(l10n);
            final openDaysTitle = isFrench ? 'Jours ouverts' : 'Open days';
            final openingHoursLabel = _translateOrFallback(
              l10n,
              'establishment_hours_title',
              isFrench ? 'Horaires d\'ouverture' : 'Opening Hours',
            );
            final openingWindowsText = _formatOpeningWindowsForDate(
              selectedDate,
              l10n,
            );
            final selectedLabel = selectedMinute >= 0
                ? '${_formatHourPart(selectedHour)}:${_formatMinutePart(selectedMinutePart)}'
                : l10n.translate('no_slot_selected');

            return AlertDialog(
              title: Text(l10n.translate('schedule_delivery')),
              content: SizedBox(
                width: double.maxFinite,
                child: ConstrainedBox(
                  constraints: BoxConstraints(
                    maxHeight: media.size.height * 0.78,
                  ),
                  child: SingleChildScrollView(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          openDaysTitle,
                          style: const TextStyle(fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(height: 10),
                        SizedBox(
                          height: 44,
                          child: ListView.separated(
                            scrollDirection: Axis.horizontal,
                            itemCount: openDates.length,
                            separatorBuilder: (_, __) =>
                                const SizedBox(width: 8),
                            itemBuilder: (_, index) {
                              final date = openDates[index];
                              final isSelected = _isSameDate(
                                date,
                                selectedDate,
                              );
                              final hasOpeningMinutes =
                                  _buildAvailableMinutesForDate(
                                    date,
                                  ).isNotEmpty;

                              return ChoiceChip(
                                label: Text(_formatScheduleDayLabel(date)),
                                selected: isSelected,
                                selectedColor: isSelected
                                    ? AppColors.secondary3
                                    : null,
                                avatar: hasOpeningMinutes
                                    ? null
                                    : const Icon(
                                        Icons.info_outline_rounded,
                                        size: 14,
                                      ),
                                onSelected: (_) {
                                  setModalState(() {
                                    selectedDate = date;
                                    final nextMinutes =
                                        _buildAvailableMinutesForDate(date);
                                    selectedMinute = nextMinutes.isEmpty
                                        ? -1
                                        : nextMinutes.first;
                                    analogSelection = _AnalogDialSelection.hour;
                                  });
                                },
                                side: BorderSide(
                                  color: hasOpeningMinutes
                                      ? Colors.black26
                                      : Colors.black12,
                                ),
                              );
                            },
                          ),
                        ),
                        const SizedBox(height: 12),
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: const Color(0xFFF7F8FA),
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: Colors.black12),
                          ),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Icon(Icons.schedule_rounded, size: 18),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  '$openingHoursLabel: $openingWindowsText',
                                  style: const TextStyle(
                                    fontSize: 12.8,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),
                        if (clockMode == _ScheduleClockMode.analog)
                          _ScheduleAnalogClockCard(
                            selectedHour: selectedHour >= 0
                                ? selectedHour
                                : null,
                            selectedMinutePart: selectedMinutePart >= 0
                                ? selectedMinutePart
                                : null,
                            enabledHours: availableHours.toSet(),
                            enabledMinuteParts: availableMinuteParts.toSet(),
                            selectionMode: analogSelection,
                            onSelectionModeChanged: (mode) {
                              setModalState(() {
                                analogSelection = mode;
                              });
                            },
                            onSelectHour: (hour) {
                              setModalState(() {
                                final minuteChoices =
                                    _availableMinutePartsForHour(
                                      availableMinutes,
                                      hour,
                                    );
                                if (minuteChoices.isEmpty) {
                                  return;
                                }

                                final nextMinutePart =
                                    minuteChoices.contains(selectedMinutePart)
                                    ? selectedMinutePart
                                    : minuteChoices.first;
                                selectedMinute = (hour * 60) + nextMinutePart;
                              });
                            },
                            onSelectMinutePart: (minutePart) {
                              setModalState(() {
                                final hour = selectedHour >= 0
                                    ? selectedHour
                                    : (availableHours.isEmpty
                                          ? -1
                                          : availableHours.first);
                                if (hour < 0) {
                                  return;
                                }
                                selectedMinute = (hour * 60) + minutePart;
                              });
                            },
                            onSwitchMode: availableMinutes.isEmpty
                                ? null
                                : () {
                                    setModalState(() {
                                      clockMode = _ScheduleClockMode.digital;
                                    });
                                  },
                          )
                        else
                          _ScheduleDigitalClockCard(
                            selectedSlotLabel: selectedLabel,
                            selectedHour: selectedHour >= 0
                                ? selectedHour
                                : null,
                            selectedMinutePart: selectedMinutePart >= 0
                                ? selectedMinutePart
                                : null,
                            availableHours: availableHours,
                            availableMinuteParts: availableMinuteParts,
                            formatHourLabel: _formatHourPart,
                            formatMinuteLabel: _formatMinutePart,
                            onSwitchMode: () {
                              setModalState(() {
                                clockMode = _ScheduleClockMode.analog;
                                analogSelection = _AnalogDialSelection.hour;
                              });
                            },
                            onSelectHour: (hour) {
                              setModalState(() {
                                final minuteChoices =
                                    _availableMinutePartsForHour(
                                      availableMinutes,
                                      hour,
                                    );
                                final nextMinutePart =
                                    minuteChoices.contains(selectedMinutePart)
                                    ? selectedMinutePart
                                    : minuteChoices.first;
                                selectedMinute = (hour * 60) + nextMinutePart;
                              });
                            },
                            onSelectMinutePart: (minutePart) {
                              setModalState(() {
                                final hour = selectedHour >= 0
                                    ? selectedHour
                                    : availableHours.first;
                                selectedMinute = (hour * 60) + minutePart;
                              });
                            },
                          ),
                        const SizedBox(height: 12),
                        if (availableMinutes.isEmpty)
                          Text(
                            l10n.translate('no_slots_this_day'),
                            style: const TextStyle(color: Colors.black54),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  style: TextButton.styleFrom(
                    foregroundColor: const Color(0xFFDC2626),
                  ),
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: Text(l10n.translate('cancel')),
                ),
                FilledButton(
                  style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFFDC2626),
                    foregroundColor: Colors.white,
                  ),
                  onPressed: selectedMinute < 0
                      ? null
                      : () {
                          final result = DateTime(
                            selectedDate.year,
                            selectedDate.month,
                            selectedDate.day,
                            selectedMinute ~/ 60,
                            selectedMinute % 60,
                          );
                          Navigator.of(dialogContext).pop(result);
                        },
                  child: Text(l10n.translate('done')),
                ),
              ],
            );
          },
        );
      },
    );

    return pickedDateTime;
  }

  Future<void> _submitOrder({
    required List<CartItemModel> items,
    required bool partnerOpen,
    required bool minNotReached,
    required bool outOfZone,
    required bool hasAddress,
    required AddressModel? effectiveSavedAddress,
    required SavedLocation? mapLocation,
  }) async {
    if (items.isEmpty || _isPlacingOrder) return;

    final l10n = AppLocalizations.of(context);

    if (minNotReached) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('minimum_order_not_reached')),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    if (!hasAddress) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('select_delivery_address')),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    if (outOfZone) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('address_out_of_delivery_zone')),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    final needsSchedule =
        !partnerOpen || _deliveryMode == _DeliveryMode.schedule;

    if (needsSchedule && _scheduledDateTime == null) {
      final picked = await _pickScheduledDateTime();
      if (picked == null) return;
      if (!mounted) return;
      setState(() => _scheduledDateTime = picked);
    }

    setState(() => _isPlacingOrder = true);
    try {
      final deliveryAddressDetails = _buildDeliveryAddressPayload(
        effectiveSavedAddress: effectiveSavedAddress,
        mapLocation: mapLocation,
      );

      final placedOrder = await ref
          .read(cartNotifierProvider.notifier)
          .placeOrder(
            promoCode: _appliedPromoCode,
            addressId: _useMapAddress
                ? null
                : effectiveSavedAddress?.id.toString(),
            deliveryAddressDetails: deliveryAddressDetails,
            paymentMethod: _paymentMethodCash,
            scheduledDeliveryTime: needsSchedule ? _scheduledDateTime : null,
          );

      if (!mounted) return;
      if (placedOrder == null || placedOrder.orderId.isEmpty) {
        await ref.read(cartNotifierProvider.notifier).clearCart();
        if (!mounted) return;
        context.go(RouteNames.orders);
        return;
      }

      await ref.read(cartNotifierProvider.notifier).clearCart();
      if (!mounted) return;

      await _showOrderSuccessPopup(placedOrder);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('${l10n.translate('order_failed_prefix')}: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) {
        setState(() => _isPlacingOrder = false);
      }
    }
  }

  Future<void> _showOrderSuccessPopup(PlacedOrderSummary placedOrder) async {
    if (!mounted) return;
    final l10n = AppLocalizations.of(context);

    await showGeneralDialog<void>(
      context: context,
      barrierLabel: l10n.translate('order_confirmation'),
      barrierDismissible: false,
      barrierColor: Colors.transparent,
      transitionDuration: const Duration(milliseconds: 280),
      pageBuilder: (dialogContext, _, __) {
        return _CheckoutOrderSuccessPopup(
          orderNumber: placedOrder.orderNumber,
          partnerName: placedOrder.partnerName,
          estimatedDeliveryTime: placedOrder.estimatedDeliveryTime,
          onTrackNow: () {
            Navigator.of(dialogContext).pop();
            if (!mounted) return;

            final orderId = placedOrder.orderId.trim();
            if (orderId.isEmpty) {
              context.go(RouteNames.orders);
              return;
            }

            context.go(RouteNames.orderTracking(orderId));
          },
          onContinueShopping: () {
            Navigator.of(dialogContext).pop();
            if (!mounted) return;
            context.go(RouteNames.explore);
          },
        );
      },
      transitionBuilder: (context, animation, _, child) {
        final curve = CurvedAnimation(
          parent: animation,
          curve: Curves.easeOutBack,
          reverseCurve: Curves.easeInCubic,
        );

        return FadeTransition(
          opacity: animation,
          child: ScaleTransition(
            scale: Tween<double>(begin: 0.92, end: 1).animate(curve),
            child: child,
          ),
        );
      },
    );
  }

  void _goToCartForEdit() {
    final navigator = Navigator.of(context);
    if (navigator.canPop()) {
      navigator.pop();
      return;
    }
    context.go(RouteNames.cart);
  }

  @override
  Widget build(BuildContext context) {
    final media = MediaQuery.of(context);
    final screenWidth = media.size.width;
    final horizontalPadding = screenWidth < 360 ? 12.0 : 16.0;
    final l10n = AppLocalizations.of(context);

    final lightTheme = Theme.of(context).copyWith(
      brightness: Brightness.light,
      scaffoldBackgroundColor: Colors.white,
      colorScheme: Theme.of(context).colorScheme.copyWith(
        brightness: Brightness.light,
        surface: Colors.white,
      ),
      appBarTheme: Theme.of(context).appBarTheme.copyWith(
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        surfaceTintColor: Colors.white,
      ),
      dialogTheme: Theme.of(context).dialogTheme.copyWith(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.white,
      ),
      bottomSheetTheme: Theme.of(context).bottomSheetTheme.copyWith(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.white,
      ),
    );

    final cartState = ref.watch(cartNotifierProvider);
    final items = cartState.items;
    final subtotal = cartState.subtotal;
    final itemCount = cartState.itemCount;

    final partnerId = items.isNotEmpty ? items.first.partnerId : null;
    _ensurePartnerLoaded(partnerId);

    final addressesState = ref.watch(addressNotifierProvider);
    final addresses = addressesState.valueOrNull ?? const <AddressModel>[];
    final mapLocation = ref.watch(locationNotifierProvider).location;

    final effectiveSavedAddress = _effectiveSavedAddress(addresses);
    final hasAddress =
        (_useMapAddress && mapLocation != null) ||
        effectiveSavedAddress != null ||
        mapLocation != null;

    final coords = _resolveCoordinates(
      addresses: addresses,
      mapLocation: mapLocation,
      effectiveSavedAddress: effectiveSavedAddress,
    );

    final outOfZone = _isCurrentPartnerOutOfZone(
      userLat: coords.lat,
      userLng: coords.lng,
    );
    final partnerOpen = _partnerInfo?.isOpen ?? true;
    final minimumOrder = _partnerInfo?.minimumOrder ?? 0;
    final minNotReached = minimumOrder > 0 && subtotal < minimumOrder;

    final serviceFee = _computeServiceFee(subtotal);
    final deliveryFee = _effectiveDeliveryFee(subtotal);
    final promoDiscount = _promoDiscount(subtotal);
    final total = (subtotal + deliveryFee + serviceFee - promoDiscount)
        .clamp(0, double.infinity)
        .toDouble();

    final needsSchedule =
        !partnerOpen || _deliveryMode == _DeliveryMode.schedule;

    final displayedItems = _showAllItems || items.length <= 2
        ? items
        : items.take(2).toList();
    final orderItemsLabel = itemCount == 1
        ? l10n.translate('item_singular')
        : l10n.translate('items_plural');

    final canSubmit =
        items.isNotEmpty &&
        !_isPlacingOrder &&
        !minNotReached &&
        !outOfZone &&
        hasAddress;

    return Theme(
      data: lightTheme,
      child: Scaffold(
        appBar: AppBar(title: Text(l10n.translate('checkout'))),
        body: items.isEmpty
            ? const _EmptyCheckoutView()
            : ListView(
                padding: EdgeInsets.fromLTRB(
                  horizontalPadding,
                  14,
                  horizontalPadding,
                  92 + media.padding.bottom,
                ),
                children: [
                  _WavySeparator(
                    title:
                        '${l10n.translate('order_section_title')} ($itemCount $orderItemsLabel)',
                    topSpacing: 0,
                  ),
                  _SectionCard(
                    trailing: Wrap(
                      spacing: 2,
                      children: [
                        TextButton.icon(
                          onPressed: _goToCartForEdit,
                          icon: const Icon(Icons.edit_outlined, size: 16),
                          label: Text(l10n.translate('edit')),
                        ),
                        if (items.length > 2)
                          TextButton(
                            onPressed: () {
                              setState(() => _showAllItems = !_showAllItems);
                            },
                            child: Text(
                              _showAllItems
                                  ? l10n.translate('collapse')
                                  : l10n.translate('show_all'),
                            ),
                          ),
                      ],
                    ),
                    child: Column(
                      children: [
                        ...displayedItems.map(
                          (item) => Padding(
                            padding: const EdgeInsets.only(bottom: 10),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Container(
                                  width: 26,
                                  height: 26,
                                  alignment: Alignment.center,
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFEFF4FF),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Text(
                                    '${item.quantity}x',
                                    style: const TextStyle(
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        item.productName,
                                        style: const TextStyle(
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                      if (item.selectedOptions.isNotEmpty)
                                        Text(
                                          item.selectedOptionsDisplay.join(
                                            ', ',
                                          ),
                                          style: const TextStyle(
                                            color: Colors.black54,
                                            fontSize: 12,
                                          ),
                                        ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  _money(item.lineTotal),
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  _WavySeparator(
                    title: l10n.translate('delivery_address'),
                    topSpacing: 16,
                  ),
                  _SectionCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (_useMapAddress && mapLocation != null)
                          _AddressPreview(
                            title:
                                mapLocation.customLabel ??
                                l10n.translate('map_address_title'),
                            subtitle: mapLocation.formattedAddress,
                            icon: Icons.map_rounded,
                          )
                        else if (effectiveSavedAddress != null)
                          _AddressPreview(
                            title:
                                '${effectiveSavedAddress.displayLabel} (${_addressTypeLabel(effectiveSavedAddress.type)})',
                            subtitle: _addressSubtitle(effectiveSavedAddress),
                            icon: _addressTypeIcon(effectiveSavedAddress.type),
                          )
                        else
                          _AddressPreview(
                            title: l10n.translate('no_address_selected'),
                            subtitle: l10n.translate(
                              'choose_saved_or_map_address',
                            ),
                            icon: Icons.location_off_rounded,
                          ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Expanded(
                              child: OutlinedButton.icon(
                                onPressed: addressesState.isLoading
                                    ? null
                                    : () => _pickSavedAddress(addresses),
                                icon: const Icon(Icons.bookmark_border_rounded),
                                label: Text(l10n.translate('saved_addresses')),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            Expanded(
                              child: OutlinedButton.icon(
                                onPressed: () => _pickAddressOnMap(
                                  addresses: addresses,
                                  effectiveSavedAddress: effectiveSavedAddress,
                                  mapLocation: mapLocation,
                                ),
                                icon: const Icon(Icons.map_rounded),
                                label: Text(l10n.translate('choose_on_map')),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  _WavySeparator(
                    title: l10n.translate('delivery'),
                    topSpacing: 16,
                  ),
                  _SectionCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (_isLoadingPartner)
                          const Padding(
                            padding: EdgeInsets.symmetric(vertical: 6),
                            child: SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            ),
                          ),
                        if (!partnerOpen)
                          Container(
                            width: double.infinity,
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: AppColors.secondary2,
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: AppColors.secondary),
                            ),
                            child: Text(
                              l10n.translate('partner_closed_schedule_only'),
                              style: const TextStyle(
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                        if (partnerOpen)
                          LayoutBuilder(
                            builder: (context, constraints) {
                              final isCompact = constraints.maxWidth < 420;

                              final nowOption = RadioListTile<_DeliveryMode>(
                                dense: true,
                                contentPadding: EdgeInsets.zero,
                                value: _DeliveryMode.now,
                                groupValue: _deliveryMode,
                                onChanged: (value) {
                                  if (value == null) return;
                                  setState(() {
                                    _deliveryMode = value;
                                    if (value == _DeliveryMode.now) {
                                      _scheduledDateTime = null;
                                    }
                                  });
                                },
                                title: Text(l10n.translate('delivery_now')),
                              );

                              final scheduleOption =
                                  RadioListTile<_DeliveryMode>(
                                    dense: true,
                                    contentPadding: EdgeInsets.zero,
                                    value: _DeliveryMode.schedule,
                                    groupValue: _deliveryMode,
                                    onChanged: (value) {
                                      if (value == null) return;
                                      setState(() {
                                        _deliveryMode = value;
                                      });
                                    },
                                    title: Text(
                                      l10n.translate('delivery_schedule'),
                                    ),
                                  );

                              if (isCompact) {
                                return Column(
                                  children: [nowOption, scheduleOption],
                                );
                              }

                              return Row(
                                children: [
                                  Expanded(child: nowOption),
                                  Expanded(child: scheduleOption),
                                ],
                              );
                            },
                          ),
                        if (needsSchedule)
                          Container(
                            width: double.infinity,
                            margin: const EdgeInsets.only(top: 4),
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF7F8FA),
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: Colors.black12),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  _scheduledDateTime == null
                                      ? l10n.translate('no_slot_selected')
                                      : '${l10n.translate('slot_label')}: ${_formatScheduleDateTime(_scheduledDateTime!)}',
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                                const SizedBox(height: 8),
                                OutlinedButton.icon(
                                  onPressed: () async {
                                    final picked =
                                        await _pickScheduledDateTime();
                                    if (picked == null || !mounted) return;
                                    setState(() => _scheduledDateTime = picked);
                                  },
                                  icon: const Icon(Icons.schedule_rounded),
                                  label: Text(l10n.translate('choose_slot')),
                                ),
                              ],
                            ),
                          ),
                      ],
                    ),
                  ),
                  _WavySeparator(
                    title: l10n.translate('payment_and_promo'),
                    topSpacing: 16,
                  ),
                  _SectionCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: Colors.black12),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.payments_outlined),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Text(
                                  l10n.translate('cash_on_delivery'),
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ),
                              const Icon(
                                Icons.check_circle,
                                color: Colors.green,
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 10),
                        LayoutBuilder(
                          builder: (context, constraints) {
                            final isCompact = constraints.maxWidth < 380;

                            final promoField = TextField(
                              controller: _promoController,
                              decoration: InputDecoration(
                                labelText: l10n.translate('promo_code_label'),
                                border: const OutlineInputBorder(),
                                isDense: true,
                              ),
                              textInputAction: TextInputAction.done,
                              onSubmitted: (_) => _applyPromo(subtotal),
                            );

                            final applyButton = ElevatedButton(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.white,
                                foregroundColor: AppColors.primary,
                                elevation: 0,
                                side: const BorderSide(color: Colors.black),
                              ),
                              onPressed: () => _applyPromo(subtotal),
                              child: Text(l10n.translate('apply')),
                            );

                            if (isCompact) {
                              return Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  promoField,
                                  const SizedBox(height: 8),
                                  SizedBox(height: 42, child: applyButton),
                                ],
                              );
                            }

                            return Row(
                              children: [
                                Expanded(child: promoField),
                                const SizedBox(width: 8),
                                applyButton,
                              ],
                            );
                          },
                        ),
                        if ((_promoMessage ?? '').isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 8),
                            child: Text(
                              _promoMessage!,
                              style: TextStyle(
                                color: _promoMessageIsError
                                    ? Colors.red
                                    : Colors.green,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                  _WavySeparator(
                    title: l10n.translate('final_summary'),
                    topSpacing: 16,
                  ),
                  _SectionCard(
                    child: Column(
                      children: [
                        _SummaryRow(
                          label: l10n.translate('products_label'),
                          value: _money(subtotal),
                        ),
                        _SummaryRow(
                          label: l10n.translate('delivery'),
                          value: _isLoadingDeliveryFee
                              ? l10n.translate('loading')
                              : _money(deliveryFee),
                        ),
                        _SummaryRow(
                          label: l10n.translate('service'),
                          value: _money(serviceFee),
                        ),
                        _SummaryRow(
                          label: l10n.translate('promotion'),
                          value: '-${_money(promoDiscount)}',
                        ),
                        const Divider(height: 22),
                        _SummaryRow(
                          label: l10n.translate('total_to_pay'),
                          value: _money(total),
                          bold: true,
                        ),
                      ],
                    ),
                  ),
                  if (minNotReached)
                    Padding(
                      padding: const EdgeInsets.only(top: 10),
                      child: Text(
                        '${l10n.translate('minimum_required')}: ${_money(minimumOrder)} (${l10n.translate('missing_amount')} ${_money(minimumOrder - subtotal)}).',
                        style: const TextStyle(
                          color: Colors.red,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  if (outOfZone)
                    Padding(
                      padding: EdgeInsets.only(top: 10),
                      child: Text(
                        l10n.translate('address_out_of_delivery_zone'),
                        style: const TextStyle(
                          color: Colors.red,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                ],
              ),
        bottomNavigationBar: items.isEmpty
            ? null
            : SafeArea(
                top: false,
                minimum: EdgeInsets.fromLTRB(
                  horizontalPadding,
                  8,
                  horizontalPadding,
                  10,
                ),
                child: SizedBox(
                  height: 50,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: Colors.white,
                    ),
                    onPressed: canSubmit
                        ? () => _submitOrder(
                            items: items,
                            partnerOpen: partnerOpen,
                            minNotReached: minNotReached,
                            outOfZone: outOfZone,
                            hasAddress: hasAddress,
                            effectiveSavedAddress: effectiveSavedAddress,
                            mapLocation: mapLocation,
                          )
                        : null,
                    child: Text(
                      _isPlacingOrder
                          ? l10n.translate('validating_order')
                          : needsSchedule
                          ? l10n.translate('place_scheduled_order')
                          : l10n.translate('place_order'),
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ),
                ),
              ),
      ),
    );
  }
}

class _WavySeparator extends StatelessWidget {
  final String title;
  final double topSpacing;
  final double bottomSpacing;

  const _WavySeparator({
    required this.title,
    this.topSpacing = 10,
    this.bottomSpacing = 6,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(top: topSpacing, bottom: bottomSpacing),
      child: SizedBox(
        height: 28,
        child: Stack(
          alignment: Alignment.center,
          children: [
            Positioned.fill(child: CustomPaint(painter: _WavyPainter())),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(999),
                border: Border.all(color: const Color(0xFFD0D4DC)),
              ),
              child: Text(
                title,
                style: const TextStyle(
                  fontWeight: FontWeight.w800,
                  fontSize: 13,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _WavyPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFFD0D4DC)
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke;

    final path = Path();
    const waveHeight = 4.0;
    const waveWidth = 8.0;

    path.moveTo(0, size.height / 2);

    for (double i = 0; i < size.width; i += waveWidth) {
      path.quadraticBezierTo(
        i + waveWidth / 2,
        size.height / 2 - waveHeight,
        i + waveWidth,
        size.height / 2,
      );
    }

    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class _SectionCard extends StatelessWidget {
  final Widget child;
  final Widget? trailing;

  const _SectionCard({required this.child, this.trailing});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (trailing != null)
              Align(alignment: Alignment.centerRight, child: trailing!),
            if (trailing != null) const SizedBox(height: 8),
            child,
          ],
        ),
      ),
    );
  }
}

class _AddressPreview extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;

  const _AddressPreview({
    required this.title,
    required this.subtitle,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFF7F8FA),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.black12),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 2),
                Text(subtitle, style: const TextStyle(color: Colors.black54)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  final String label;
  final String value;
  final bool bold;

  const _SummaryRow({
    required this.label,
    required this.value,
    this.bold = false,
  });

  @override
  Widget build(BuildContext context) {
    final style = TextStyle(
      fontWeight: bold ? FontWeight.w800 : FontWeight.w600,
      fontSize: bold ? 16 : 14,
    );

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Expanded(child: Text(label, style: style)),
          Text(value, style: style),
        ],
      ),
    );
  }
}

class _EmptyCheckoutView extends StatelessWidget {
  const _EmptyCheckoutView();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.remove_shopping_cart_outlined,
              size: 52,
              color: Colors.black38,
            ),
            const SizedBox(height: 8),
            Text(
              l10n.translate('no_products_in_cart'),
              style: TextStyle(fontWeight: FontWeight.w800, fontSize: 18),
            ),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: () => context.pop(),
              child: Text(l10n.translate('back_to_cart')),
            ),
          ],
        ),
      ),
    );
  }
}

class _ScheduleDigitalClockCard extends StatelessWidget {
  final String selectedSlotLabel;
  final int? selectedHour;
  final int? selectedMinutePart;
  final List<int> availableHours;
  final List<int> availableMinuteParts;
  final String Function(int) formatHourLabel;
  final String Function(int) formatMinuteLabel;
  final VoidCallback onSwitchMode;
  final ValueChanged<int> onSelectHour;
  final ValueChanged<int> onSelectMinutePart;

  const _ScheduleDigitalClockCard({
    required this.selectedSlotLabel,
    required this.selectedHour,
    required this.selectedMinutePart,
    required this.availableHours,
    required this.availableMinuteParts,
    required this.formatHourLabel,
    required this.formatMinuteLabel,
    required this.onSwitchMode,
    required this.onSelectHour,
    required this.onSelectMinutePart,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white),
      ),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final compact = constraints.maxWidth < 360;
          final slotFontSize = compact ? 18.0 : 20.0;
          final wheelHeight = compact ? 194.0 : 210.0;

          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.access_time_filled_rounded,
                    size: 18,
                    color: Color(0xFFDC2626),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      selectedSlotLabel,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: slotFontSize,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 1.1,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: _TimeWheelColumn(
                      label: 'HH',
                      values: availableHours,
                      selectedValue: selectedHour,
                      formatValue: formatHourLabel,
                      onSelect: onSelectHour,
                      height: wheelHeight,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _TimeWheelColumn(
                      label: 'MM',
                      values: availableMinuteParts,
                      selectedValue: selectedMinutePart,
                      formatValue: formatMinuteLabel,
                      onSelect: onSelectMinutePart,
                      height: wheelHeight,
                    ),
                  ),
                ],
              ),
              Align(
                alignment: Alignment.centerLeft,
                child: IconButton(
                  onPressed: onSwitchMode,
                  color: const Color(0xFFDC2626),
                  icon: const Icon(Icons.watch_later_outlined),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _ScheduleAnalogClockCard extends StatelessWidget {
  final int? selectedHour;
  final int? selectedMinutePart;
  final Set<int> enabledHours;
  final Set<int> enabledMinuteParts;
  final _AnalogDialSelection selectionMode;
  final ValueChanged<_AnalogDialSelection> onSelectionModeChanged;
  final ValueChanged<int> onSelectHour;
  final ValueChanged<int> onSelectMinutePart;
  final VoidCallback? onSwitchMode;

  const _ScheduleAnalogClockCard({
    required this.selectedHour,
    required this.selectedMinutePart,
    required this.enabledHours,
    required this.enabledMinuteParts,
    required this.selectionMode,
    required this.onSelectionModeChanged,
    required this.onSelectHour,
    required this.onSelectMinutePart,
    this.onSwitchMode,
  });

  String _twoDigits(int? value) {
    if (value == null || value < 0) return '--';
    return value.toString().padLeft(2, '0');
  }

  @override
  Widget build(BuildContext context) {
    final displayHour = _twoDigits(selectedHour);
    final displayMinute = _twoDigits(selectedMinutePart);

    return LayoutBuilder(
      builder: (context, constraints) {
        final isCompact = constraints.maxWidth < 300;
        final chipHeight = isCompact ? 66.0 : 74.0;
        final digitFontSize = isCompact ? 40.0 : 54.0;
        final colonFontSize = isCompact ? 42.0 : 58.0;
        final analogSize = constraints.maxWidth < 360 ? 216.0 : 246.0;

        return Container(
          width: double.infinity,
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.white),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Row(
                  children: [
                    Expanded(
                      child: _AlarmTimeChip(
                        value: displayHour,
                        selected: selectionMode == _AnalogDialSelection.hour,
                        onTap: () =>
                            onSelectionModeChanged(_AnalogDialSelection.hour),
                        width: double.infinity,
                        height: chipHeight,
                        fontSize: digitFontSize,
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.symmetric(
                        horizontal: isCompact ? 2 : 4,
                      ),
                      child: Text(
                        ':',
                        style: TextStyle(
                          fontSize: colonFontSize,
                          fontWeight: FontWeight.w300,
                          color: const Color(0xFF6B7280),
                          height: 1,
                        ),
                      ),
                    ),
                    Expanded(
                      child: _AlarmTimeChip(
                        value: displayMinute,
                        selected: selectionMode == _AnalogDialSelection.minute,
                        onTap: () =>
                            onSelectionModeChanged(_AnalogDialSelection.minute),
                        width: double.infinity,
                        height: chipHeight,
                        fontSize: digitFontSize,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              Center(
                child: _OpeningHoursAnalogClock(
                  selectionMode: selectionMode,
                  selectedHour: selectedHour,
                  selectedMinutePart: selectedMinutePart,
                  enabledHours: enabledHours,
                  enabledMinuteParts: enabledMinuteParts,
                  onSelectHour: onSelectHour,
                  onSelectMinutePart: onSelectMinutePart,
                  size: analogSize,
                ),
              ),
              Align(
                alignment: Alignment.centerLeft,
                child: IconButton(
                  onPressed: onSwitchMode,
                  color: const Color(0xFFDC2626),
                  icon: const Icon(Icons.keyboard_outlined),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _AlarmTimeChip extends StatelessWidget {
  final String value;
  final bool selected;
  final VoidCallback onTap;
  final double width;
  final double height;
  final double fontSize;

  const _AlarmTimeChip({
    required this.value,
    required this.selected,
    required this.onTap,
    this.width = 106,
    this.height = 74,
    this.fontSize = 54,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: width,
        height: height,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: Colors.white),
        ),
        child: Stack(
          alignment: Alignment.center,
          children: [
            Text(
              value,
              style: TextStyle(
                fontSize: fontSize,
                fontWeight: FontWeight.w300,
                height: 1,
                color: selected
                    ? const Color(0xFF111827)
                    : const Color(0xFF9CA3AF),
              ),
            ),
            if (selected)
              Positioned(
                bottom: 3,
                child: Container(
                  width: (fontSize * 0.62).clamp(24, 34).toDouble(),
                  height: 2.8,
                  decoration: BoxDecoration(
                    color: const Color(0xFFDC2626),
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _TimeWheelColumn extends StatefulWidget {
  final String label;
  final List<int> values;
  final int? selectedValue;
  final String Function(int) formatValue;
  final ValueChanged<int> onSelect;
  final double height;

  const _TimeWheelColumn({
    required this.label,
    required this.values,
    required this.selectedValue,
    required this.formatValue,
    required this.onSelect,
    required this.height,
  });

  @override
  State<_TimeWheelColumn> createState() => _TimeWheelColumnState();
}

class _TimeWheelColumnState extends State<_TimeWheelColumn> {
  late FixedExtentScrollController _controller;
  bool _suppressSelectionCallback = false;

  int _currentIndex() {
    if (widget.values.isEmpty) return 0;
    final value = widget.selectedValue;
    if (value == null) return 0;
    final index = widget.values.indexOf(value);
    return index < 0 ? 0 : index;
  }

  @override
  void initState() {
    super.initState();
    _controller = FixedExtentScrollController(initialItem: _currentIndex());
  }

  void _releaseSuppressionNextFrame() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _suppressSelectionCallback = false;
    });
  }

  void _handleWheelSelectionChanged(int index) {
    if (_suppressSelectionCallback) return;
    if (index < 0 || index >= widget.values.length) return;

    final selected = widget.values[index];
    if (selected == widget.selectedValue) return;

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || _suppressSelectionCallback) return;
      if (!widget.values.contains(selected)) return;
      if (selected == widget.selectedValue) return;
      widget.onSelect(selected);
    });
  }

  @override
  void didUpdateWidget(covariant _TimeWheelColumn oldWidget) {
    super.didUpdateWidget(oldWidget);
    final nextIndex = _currentIndex();
    if (_controller.hasClients) {
      final currentIndex = _controller.selectedItem;
      if (currentIndex != nextIndex) {
        _suppressSelectionCallback = true;
        _controller.jumpToItem(nextIndex);
        _releaseSuppressionNextFrame();
      }
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.values.isEmpty) {
      return Container(
        height: widget.height,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: Colors.white),
        ),
        child: Column(
          children: [
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(10)),
              ),
              child: Text(
                widget.label,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 0.8,
                ),
              ),
            ),
            const Expanded(
              child: Center(
                child: Text(
                  '--',
                  style: TextStyle(
                    color: Colors.black38,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
          ],
        ),
      );
    }

    return Container(
      height: widget.height,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.white),
      ),
      child: Column(
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.vertical(top: Radius.circular(10)),
            ),
            child: Text(
              widget.label,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w800,
                letterSpacing: 0.8,
              ),
            ),
          ),
          Expanded(
            child: ListWheelScrollView.useDelegate(
              controller: _controller,
              itemExtent: 34,
              diameterRatio: 1.45,
              physics: const FixedExtentScrollPhysics(),
              onSelectedItemChanged: _handleWheelSelectionChanged,
              childDelegate: ListWheelChildBuilderDelegate(
                childCount: widget.values.length,
                builder: (context, index) {
                  if (index < 0 || index >= widget.values.length) return null;
                  final value = widget.values[index];
                  final isSelected = value == widget.selectedValue;
                  return Center(
                    child: Text(
                      widget.formatValue(value),
                      style: TextStyle(
                        fontSize: isSelected ? 20 : 16,
                        fontWeight: isSelected
                            ? FontWeight.w900
                            : FontWeight.w600,
                        color: isSelected ? Color(0xFFDC2626) : Colors.black54,
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _OpeningHoursAnalogClock extends StatefulWidget {
  final _AnalogDialSelection selectionMode;
  final int? selectedHour;
  final int? selectedMinutePart;
  final Set<int> enabledHours;
  final Set<int> enabledMinuteParts;
  final ValueChanged<int> onSelectHour;
  final ValueChanged<int> onSelectMinutePart;
  final double size;

  const _OpeningHoursAnalogClock({
    required this.selectionMode,
    required this.selectedHour,
    required this.selectedMinutePart,
    required this.enabledHours,
    required this.enabledMinuteParts,
    required this.onSelectHour,
    required this.onSelectMinutePart,
    this.size = 120,
  });

  @override
  State<_OpeningHoursAnalogClock> createState() =>
      _OpeningHoursAnalogClockState();
}

class _OpeningHoursAnalogClockState extends State<_OpeningHoursAnalogClock> {
  double _angleForDivisions(int value, int divisions) {
    return ((value / divisions) * (2 * math.pi)) - (math.pi / 2);
  }

  bool _isInnerHour(int hour) => hour == 0 || hour > 12;

  Offset _hourPoint(Offset center, double dialRadius, int hour) {
    final angle = _angleForDivisions(hour % 12, 12);
    final radius = _isInnerHour(hour) ? dialRadius - 58 : dialRadius - 24;
    return Offset(
      center.dx + (math.cos(angle) * radius),
      center.dy + (math.sin(angle) * radius),
    );
  }

  Offset _minutePoint(Offset center, double dialRadius, int minutePart) {
    final angle = _angleForDivisions(minutePart % 60, 60);
    final radius = dialRadius - 24;
    return Offset(
      center.dx + (math.cos(angle) * radius),
      center.dy + (math.sin(angle) * radius),
    );
  }

  double _dialAngleFromPosition({
    required Offset center,
    required Offset localPosition,
  }) {
    final dx = localPosition.dx - center.dx;
    final dy = localPosition.dy - center.dy;
    var angle = math.atan2(dy, dx) + (math.pi / 2);
    if (angle < 0) {
      angle += 2 * math.pi;
    }
    return angle;
  }

  int _nearestDialIndex({required double angle, required int divisions}) {
    final raw = ((angle / (2 * math.pi)) * divisions).round();
    return raw % divisions;
  }

  double _angularDistanceSteps({
    required int a,
    required int b,
    required int divisions,
  }) {
    final linear = (a - b).abs();
    return math.min(linear, divisions - linear).toDouble();
  }

  int? _nearestHourForTap(Offset localPosition) {
    final candidates = widget.enabledHours.isNotEmpty
        ? widget.enabledHours.toList(growable: false)
        : List<int>.generate(24, (index) => index);
    if (candidates.isEmpty) return null;

    final center = Offset(widget.size / 2, widget.size / 2);
    final dialRadius = (widget.size / 2) - 4;
    final angle = _dialAngleFromPosition(
      center: center,
      localPosition: localPosition,
    );
    final targetIndex = _nearestDialIndex(angle: angle, divisions: 12);

    final distanceFromCenter = (localPosition - center).distance;
    final outerHourRadius = dialRadius - 24;
    final innerHourRadius = dialRadius - 58;
    final ringThreshold = (outerHourRadius + innerHourRadius) / 2;
    final prefersInnerRing = distanceFromCenter <= ringThreshold;

    var nearest = candidates.first;
    var bestScore = double.infinity;

    for (final hour in candidates) {
      final hourIndex = hour % 12;
      final angleScore = _angularDistanceSteps(
        a: hourIndex,
        b: targetIndex,
        divisions: 12,
      );
      final ringPenalty = _isInnerHour(hour) == prefersInnerRing ? 0.0 : 0.35;
      var score = angleScore + ringPenalty;

      if (widget.selectedHour != null) {
        score +=
            _angularDistanceSteps(
              a: hourIndex,
              b: widget.selectedHour! % 12,
              divisions: 12,
            ) *
            0.001;
      }

      if (score < bestScore) {
        bestScore = score;
        nearest = hour;
      }
    }

    return nearest;
  }

  int? _nearestMinutePartForTap(Offset localPosition) {
    final candidates = widget.enabledMinuteParts.isNotEmpty
        ? widget.enabledMinuteParts.toList(growable: false)
        : List<int>.generate(60, (index) => index);
    if (candidates.isEmpty) return null;

    final center = Offset(widget.size / 2, widget.size / 2);
    final dialRadius = (widget.size / 2) - 4;
    var nearest = candidates.first;
    var bestDistance = double.infinity;

    for (final minute in candidates) {
      final point = _minutePoint(center, dialRadius, minute);
      final distance = (localPosition - point).distanceSquared;
      if (distance < bestDistance) {
        bestDistance = distance;
        nearest = minute;
      }
    }

    return nearest;
  }

  void _selectFromPosition(Offset localPosition) {
    if (widget.selectionMode == _AnalogDialSelection.hour) {
      final hour = _nearestHourForTap(localPosition);
      if (hour != null) {
        widget.onSelectHour(hour);
      }
      return;
    }

    final minutePart = _nearestMinutePartForTap(localPosition);
    if (minutePart != null) {
      widget.onSelectMinutePart(minutePart);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: widget.size,
      height: widget.size,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: (details) {
          _selectFromPosition(details.localPosition);
        },
        onPanStart: (details) {
          _selectFromPosition(details.localPosition);
        },
        onPanUpdate: (details) {
          _selectFromPosition(details.localPosition);
        },
        child: CustomPaint(
          painter: _OpeningHoursAnalogClockPainter(
            selectionMode: widget.selectionMode,
            selectedHour: widget.selectedHour,
            selectedMinutePart: widget.selectedMinutePart,
            enabledHours: widget.enabledHours,
            enabledMinuteParts: widget.enabledMinuteParts,
          ),
        ),
      ),
    );
  }
}

class _OpeningHoursAnalogClockPainter extends CustomPainter {
  static const Color _accentColor = Color(0xFFDC2626);

  final _AnalogDialSelection selectionMode;
  final int? selectedHour;
  final int? selectedMinutePart;
  final Set<int> enabledHours;
  final Set<int> enabledMinuteParts;

  const _OpeningHoursAnalogClockPainter({
    required this.selectionMode,
    required this.selectedHour,
    required this.selectedMinutePart,
    required this.enabledHours,
    required this.enabledMinuteParts,
  });

  double _angleForDivisions(int value, int divisions) {
    return ((value / divisions) * (2 * math.pi)) - (math.pi / 2);
  }

  bool _isInnerHour(int hour) => hour == 0 || hour > 12;

  String _hourDisplayLabel(int hour) {
    if (hour == 0) return '00';
    return '$hour';
  }

  Offset _hourPoint(Offset center, double dialRadius, int hour) {
    final angle = _angleForDivisions(hour % 12, 12);
    final radius = _isInnerHour(hour) ? dialRadius - 58 : dialRadius - 24;
    return Offset(
      center.dx + (math.cos(angle) * radius),
      center.dy + (math.sin(angle) * radius),
    );
  }

  Offset _minutePoint(Offset center, double dialRadius, int minutePart) {
    final angle = _angleForDivisions(minutePart % 60, 60);
    final radius = dialRadius - 24;
    return Offset(
      center.dx + (math.cos(angle) * radius),
      center.dy + (math.sin(angle) * radius),
    );
  }

  void _paintCenteredText(
    Canvas canvas,
    Offset center,
    String text,
    TextStyle style,
  ) {
    final painter = TextPainter(
      text: TextSpan(text: text, style: style),
      textDirection: TextDirection.ltr,
    )..layout();
    painter.paint(
      canvas,
      center - Offset(painter.width / 2, painter.height / 2),
    );
  }

  bool _isMinuteMarkerEnabled(int marker) {
    if (enabledMinuteParts.isEmpty) return true;
    if (enabledMinuteParts.contains(marker)) return true;

    for (var delta = 1; delta <= 2; delta++) {
      final plus = (marker + delta) % 60;
      final minus = (marker - delta + 60) % 60;
      if (enabledMinuteParts.contains(plus) ||
          enabledMinuteParts.contains(minus)) {
        return true;
      }
    }

    return false;
  }

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    final dialRadius = (math.min(size.width, size.height) / 2) - 4;

    final facePaint = Paint()..color = const Color(0xFFFFFFFF);
    canvas.drawCircle(center, dialRadius, facePaint);

    final borderPaint = Paint()
      ..color = const Color(0xFFE5E7EB)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.5;
    canvas.drawCircle(center, dialRadius, borderPaint);

    final selectedHandPaint = Paint()
      ..color = _accentColor
      ..strokeWidth = 3.2
      ..strokeCap = StrokeCap.round;

    if (selectionMode == _AnalogDialSelection.hour) {
      for (var displayHour = 1; displayHour <= 12; displayHour++) {
        final hour = displayHour;
        final point = _hourPoint(center, dialRadius, hour);
        final enabled = enabledHours.contains(hour);
        _paintCenteredText(
          canvas,
          point,
          '$displayHour',
          TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w500,
            color: enabled ? _accentColor : const Color(0xFF9CA3AF),
          ),
        );
      }

      for (var hour = 0; hour < 24; hour++) {
        if (!_isInnerHour(hour)) continue;
        final point = _hourPoint(center, dialRadius, hour);
        final enabled = enabledHours.contains(hour);
        _paintCenteredText(
          canvas,
          point,
          _hourDisplayLabel(hour),
          TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w500,
            color: enabled
                ? _accentColor.withValues(alpha: 0.78)
                : const Color(0xFF9CA3AF),
          ),
        );
      }

      if (selectedHour != null) {
        final point = _hourPoint(center, dialRadius, selectedHour!);
        canvas.drawLine(center, point, selectedHandPaint);
        canvas.drawCircle(center, 5.2, Paint()..color = _accentColor);

        final bubbleRadius = _isInnerHour(selectedHour!) ? 20.0 : 24.0;
        canvas.drawCircle(point, bubbleRadius, Paint()..color = _accentColor);

        _paintCenteredText(
          canvas,
          point,
          _isInnerHour(selectedHour!)
              ? _hourDisplayLabel(selectedHour!)
              : '${selectedHour!}',
          const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: Colors.white,
          ),
        );
      } else {
        canvas.drawCircle(center, 5.2, Paint()..color = _accentColor);
      }
    } else {
      for (var marker = 0; marker < 60; marker += 5) {
        final point = _minutePoint(center, dialRadius, marker);
        final enabled = _isMinuteMarkerEnabled(marker);
        _paintCenteredText(
          canvas,
          point,
          marker.toString().padLeft(2, '0'),
          TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w500,
            color: enabled ? _accentColor : const Color(0xFF9CA3AF),
          ),
        );
      }

      if (selectedMinutePart != null) {
        final point = _minutePoint(center, dialRadius, selectedMinutePart!);
        canvas.drawLine(center, point, selectedHandPaint);
        canvas.drawCircle(center, 5.2, Paint()..color = _accentColor);
        canvas.drawCircle(point, 24, Paint()..color = _accentColor);

        _paintCenteredText(
          canvas,
          point,
          selectedMinutePart!.toString().padLeft(2, '0'),
          const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: Colors.white,
          ),
        );
      } else {
        canvas.drawCircle(center, 5.2, Paint()..color = _accentColor);
      }
    }
  }

  bool _sameSet(Set<int> a, Set<int> b) {
    if (a.length != b.length) return false;
    for (final value in a) {
      if (!b.contains(value)) return false;
    }
    return true;
  }

  @override
  bool shouldRepaint(covariant _OpeningHoursAnalogClockPainter oldDelegate) {
    return oldDelegate.selectionMode != selectionMode ||
        oldDelegate.selectedHour != selectedHour ||
        oldDelegate.selectedMinutePart != selectedMinutePart ||
        !_sameSet(oldDelegate.enabledHours, enabledHours) ||
        !_sameSet(oldDelegate.enabledMinuteParts, enabledMinuteParts);
  }
}

class _PartnerTimeWindow {
  final int startMinutes;
  final int endMinutes;

  const _PartnerTimeWindow({
    required this.startMinutes,
    required this.endMinutes,
  });
}

class _CheckoutOrderSuccessPopup extends StatefulWidget {
  final String? orderNumber;
  final String? partnerName;
  final DateTime? estimatedDeliveryTime;
  final VoidCallback onTrackNow;
  final VoidCallback onContinueShopping;

  const _CheckoutOrderSuccessPopup({
    this.orderNumber,
    this.partnerName,
    this.estimatedDeliveryTime,
    required this.onTrackNow,
    required this.onContinueShopping,
  });

  @override
  State<_CheckoutOrderSuccessPopup> createState() =>
      _CheckoutOrderSuccessPopupState();
}

class _CheckoutOrderSuccessPopupState extends State<_CheckoutOrderSuccessPopup>
    with SingleTickerProviderStateMixin {
  static const int _autoRedirectSeconds = 5;

  Timer? _timer;
  late final AnimationController _iconController;
  int _countdown = _autoRedirectSeconds;
  bool _handled = false;

  @override
  void initState() {
    super.initState();
    _iconController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2100),
    )..repeat();

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return;

      if (_countdown <= 1) {
        setState(() => _countdown = 0);
        timer.cancel();
        _handleTrackNow();
        return;
      }

      setState(() => _countdown -= 1);
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _iconController.dispose();
    super.dispose();
  }

  void _handleTrackNow() {
    if (_handled) return;
    _handled = true;
    _timer?.cancel();
    widget.onTrackNow();
  }

  void _handleContinueShopping() {
    if (_handled) return;
    _handled = true;
    _timer?.cancel();
    widget.onContinueShopping();
  }

  String _formatEta(DateTime? value, AppLocalizations l10n) {
    if (value == null) return l10n.translate('order_confirmation_eta_soon');
    final hour = value.hour.toString().padLeft(2, '0');
    final minute = value.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final progress = (_countdown / _autoRedirectSeconds).clamp(0.0, 1.0);
    final compact = MediaQuery.of(context).size.height < 700;
    final cardMaxWidth = MediaQuery.of(context).size.width < 390
        ? 332.0
        : 348.0;

    return Material(
      color: Colors.transparent,
      child: Stack(
        children: [
          Positioned.fill(
            child: BackdropFilter(
              filter: ImageFilter.blur(sigmaX: 13, sigmaY: 13),
              child: Container(color: AppColors.black.withOpacity(0.38)),
            ),
          ),
          SafeArea(
            child: Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 10,
                ),
                child: ConstrainedBox(
                  constraints: BoxConstraints(maxWidth: cardMaxWidth),
                  child: Container(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 14),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [AppColors.surface, AppColors.surfaceLight],
                      ),
                      borderRadius: BorderRadius.circular(24),
                      border: Border.all(color: AppColors.softGrey),
                      boxShadow: const [
                        BoxShadow(
                          color: AppColors.shadow,
                          blurRadius: 22,
                          offset: Offset(0, 10),
                        ),
                      ],
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        AnimatedBuilder(
                          animation: _iconController,
                          builder: (context, child) {
                            final t = _iconController.value;
                            final pulse =
                                0.96 + (0.04 * math.sin(t * math.pi * 2));
                            final floatY = 2.2 * math.sin(t * math.pi * 2);

                            return Transform.translate(
                              offset: Offset(0, floatY),
                              child: Transform.scale(
                                scale: pulse,
                                child: SizedBox(
                                  width: 80,
                                  height: 80,
                                  child: CustomPaint(
                                    painter: _FloatingBandsPainter(progress: t),
                                    child: Center(
                                      child: Container(
                                        width: 58,
                                        height: 58,
                                        decoration: BoxDecoration(
                                          shape: BoxShape.circle,
                                          color: AppColors.secondary2,
                                          border: Border.all(
                                            color: AppColors.primary,
                                            width: 1.3,
                                          ),
                                        ),
                                        child: const Icon(
                                          Icons.celebration_rounded,
                                          color: AppColors.primary,
                                          size: 26,
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            );
                          },
                        ),
                        const SizedBox(height: 2),
                        Text(
                          l10n.translate('order_confirmation_success_title'),
                          style: const TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.w800,
                            color: AppColors.textPrimary,
                            letterSpacing: -0.2,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          widget.orderNumber == null ||
                                  widget.orderNumber!.trim().isEmpty
                              ? l10n.translate(
                                  'order_confirmation_success_message',
                                )
                              : '${l10n.translate('order_number_prefix')}${widget.orderNumber} ${l10n.translate('order_confirmation_success_suffix')}',
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 13,
                            color: AppColors.textSecondary,
                            fontWeight: FontWeight.w600,
                            height: 1.25,
                          ),
                        ),
                        const SizedBox(height: 10),
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 10,
                          ),
                          decoration: BoxDecoration(
                            color: AppColors.secondary2,
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: AppColors.border),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              if ((widget.partnerName ?? '').trim().isNotEmpty)
                                Text(
                                  widget.partnerName!.trim(),
                                  style: const TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w700,
                                    color: AppColors.textPrimary,
                                  ),
                                ),
                              if ((widget.partnerName ?? '').trim().isNotEmpty)
                                const SizedBox(height: 4),
                              Row(
                                children: [
                                  const Icon(
                                    Icons.schedule_rounded,
                                    color: AppColors.primary,
                                    size: 15,
                                  ),
                                  const SizedBox(width: 5),
                                  Text(
                                    '${l10n.translate('order_confirmation_estimated_delivery')}: ${_formatEta(widget.estimatedDeliveryTime, l10n)}',
                                    style: const TextStyle(
                                      fontSize: 12.8,
                                      color: AppColors.textSecondary,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 10),
                        Text(
                          '${l10n.translate('order_confirmation_opening_tracking_in')} $_countdown ${l10n.translate('sec')}',
                          style: const TextStyle(
                            fontSize: 12.8,
                            color: AppColors.textSecondary,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 6),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(999),
                          child: LinearProgressIndicator(
                            minHeight: 6,
                            value: progress,
                            backgroundColor: AppColors.softGrey,
                            valueColor: const AlwaysStoppedAnimation<Color>(
                              AppColors.primary,
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: OutlinedButton(
                                onPressed: _handleContinueShopping,
                                style: OutlinedButton.styleFrom(
                                  foregroundColor: AppColors.textPrimary,
                                  side: const BorderSide(
                                    color: AppColors.border,
                                  ),
                                  minimumSize: Size.fromHeight(
                                    compact ? 44 : 45,
                                  ),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                ),
                                child: Text(
                                  l10n.translate('continue_shopping'),
                                ),
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: FilledButton.icon(
                                onPressed: _handleTrackNow,
                                style: FilledButton.styleFrom(
                                  backgroundColor: AppColors.primary,
                                  foregroundColor: AppColors.surface,
                                  minimumSize: Size.fromHeight(
                                    compact ? 44 : 45,
                                  ),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                ),
                                icon: const Icon(
                                  Icons.my_location_rounded,
                                  size: 17,
                                ),
                                label: Text(l10n.translate('track_order')),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _FloatingBandsPainter extends CustomPainter {
  final double progress;

  const _FloatingBandsPainter({required this.progress});

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    final ringPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2
      ..strokeCap = StrokeCap.round;

    final radii = <double>[29, 33, 37];
    final base = progress * math.pi * 2;

    for (var i = 0; i < radii.length; i++) {
      final wave = 0.5 + (0.5 * math.sin(base + (i * 1.35)));
      final opacity = (0.16 + (wave * 0.32)).clamp(0.16, 0.48).toDouble();

      ringPaint.color = AppColors.primary.withOpacity(opacity);
      final start = base + (i * 1.7);

      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radii[i]),
        start,
        0.58,
        false,
        ringPaint,
      );
    }

    final dotPaint = Paint()
      ..style = PaintingStyle.fill
      ..color = AppColors.primary.withOpacity(0.5);

    final dots = <Offset>[
      Offset(
        center.dx + (math.cos(base) * 35),
        center.dy + (math.sin(base) * 24),
      ),
      Offset(
        center.dx + (math.cos(base + 2.2) * 33),
        center.dy + (math.sin(base + 2.2) * 22),
      ),
      Offset(
        center.dx + (math.cos(base + 4.1) * 31),
        center.dy + (math.sin(base + 4.1) * 20),
      ),
    ];

    for (final dot in dots) {
      canvas.drawCircle(dot, 2.1, dotPaint);
    }
  }

  @override
  bool shouldRepaint(covariant _FloatingBandsPainter oldDelegate) {
    return oldDelegate.progress != progress;
  }
}
