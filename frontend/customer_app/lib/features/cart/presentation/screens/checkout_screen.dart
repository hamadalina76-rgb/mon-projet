import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../config/dependency_injection/injection.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/utils/delivery_zone_utils.dart';
import '../../../location/data/models/saved_location.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../../profile/data/models/address_model.dart';
import '../../../profile/presentation/providers/address_provider.dart';
import '../../cart_providers.dart';
import '../../data/models/cart_item_model.dart';
import '../../data/repositories/cart_repository.dart';

class CheckoutScreen extends ConsumerStatefulWidget {
  const CheckoutScreen({super.key});

  @override
  ConsumerState<CheckoutScreen> createState() => _CheckoutScreenState();
}

enum _DeliveryMode {
  now,
  schedule,
}

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
    ref.read(authNotifierProvider).whenOrNull(
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

      DeliveryFeeInfo? feeInfo;
      try {
        feeInfo = await repo.fetchDeliveryFee(partnerId);
      } catch (_) {
        feeInfo = null;
      }

      if (!mounted) return;
      setState(() {
        _partnerInfo = info;
        _isLoadingPartner = false;
        _deliveryFee = feeInfo?.deliveryFee;
        _freeDeliveryThreshold = feeInfo?.freeDeliveryThreshold ?? 0;
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
        'label': (customLabel == null || customLabel.isEmpty) ? 'Adresse map' : customLabel,
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
      await ref.read(locationNotifierProvider.notifier).selectAddress(
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
        'initialAddress': mapLocation?.formattedAddress ??
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
    final previousDayName = _dayNameForDate(date.subtract(const Duration(days: 1)));

    final windows = <_PartnerTimeWindow>[];

    for (final hour in info.openingHours) {
      if (hour.dayOfWeek.toUpperCase() != dayName || hour.isClosed) continue;

      if (hour.is24Hours) {
        windows.add(const _PartnerTimeWindow(startMinutes: 0, endMinutes: 1439));
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

  List<_PartnerTimeWindow> _mergeOverlappingWindows(List<_PartnerTimeWindow> windows) {
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
    final startOfWeek = today.subtract(Duration(days: today.weekday - DateTime.monday));

    return List<DateTime>.generate(
      7,
      (index) => startOfWeek.add(Duration(days: index)),
    );
  }

  List<_HalfHourInterval> _buildHalfHourIntervalsForDate(DateTime date) {
    final target = _stripTime(date);
    final today = _stripTime(DateTime.now());
    if (target.isBefore(today)) {
      return const <_HalfHourInterval>[];
    }

    final windows = _windowsForDate(target);
    if (windows.isEmpty) {
      return const <_HalfHourInterval>[];
    }

    final now = DateTime.now();
    final minAllowed = _isSameDate(target, now)
        ? (now.hour * 60) + now.minute + _scheduleLeadMinutes
        : 0;

    final intervals = <_HalfHourInterval>[];
    final seenStarts = <int>{};

    for (final window in windows) {
      var minute = window.startMinutes;
      if (minute < minAllowed) {
        final missing = minAllowed - minute;
        minute += (missing / 30).ceil() * 30;
      }

      while (minute + 30 <= window.endMinutes) {
        if (seenStarts.add(minute)) {
          intervals.add(_HalfHourInterval(startMinutes: minute, endMinutes: minute + 30));
        }
        minute += 30;
      }
    }

    intervals.sort((a, b) => a.startMinutes.compareTo(b.startMinutes));
    return intervals;
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

  String _formatHalfHourInterval(_HalfHourInterval interval) {
    return '${_formatMinutesAsTime(interval.startMinutes)} - ${_formatMinutesAsTime(interval.endMinutes)}';
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

    final weekDates = _currentWeekDates();

    DateTime? initialDate;
    for (final date in weekDates) {
      if (_buildHalfHourIntervalsForDate(date).isNotEmpty) {
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
    var selectedIntervalStart = _buildHalfHourIntervalsForDate(selectedDate).first.startMinutes;

    final pickedDateTime = await showDialog<DateTime>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (dialogContext, setModalState) {
            final media = MediaQuery.of(dialogContext);
            final intervals = _buildHalfHourIntervalsForDate(selectedDate);
            if (!intervals.any((e) => e.startMinutes == selectedIntervalStart)) {
              selectedIntervalStart = intervals.isEmpty ? -1 : intervals.first.startMinutes;
            }

            return AlertDialog(
              title: Text(l10n.translate('schedule_delivery')),
              content: SizedBox(
                width: double.maxFinite,
                child: ConstrainedBox(
                  constraints: BoxConstraints(maxHeight: media.size.height * 0.62),
                  child: SingleChildScrollView(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(l10n.translate('current_week')),
                        const SizedBox(height: 10),
                        SizedBox(
                          height: 44,
                          child: ListView.separated(
                            scrollDirection: Axis.horizontal,
                            itemCount: weekDates.length,
                            separatorBuilder: (_, __) => const SizedBox(width: 8),
                            itemBuilder: (_, index) {
                              final date = weekDates[index];
                              final isSelected = _isSameDate(date, selectedDate);
                              final hasSlots = _buildHalfHourIntervalsForDate(date).isNotEmpty;

                              return ChoiceChip(
                                label: Text(_formatScheduleDayLabel(date)),
                                selected: isSelected,
                                onSelected: (_) {
                                  setModalState(() {
                                    selectedDate = date;
                                    final nextIntervals = _buildHalfHourIntervalsForDate(date);
                                    selectedIntervalStart =
                                        nextIntervals.isEmpty ? -1 : nextIntervals.first.startMinutes;
                                  });
                                },
                                side: BorderSide(
                                  color: hasSlots ? Colors.black26 : Colors.black12,
                                ),
                              );
                            },
                          ),
                        ),
                        const SizedBox(height: 12),
                        if (intervals.isEmpty)
                          Text(
                            l10n.translate('no_slots_this_day'),
                            style: const TextStyle(color: Colors.black54),
                          )
                        else
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: intervals.map((interval) {
                              final isSelected = interval.startMinutes == selectedIntervalStart;
                              return ChoiceChip(
                                label: Text(_formatHalfHourInterval(interval)),
                                selected: isSelected,
                                onSelected: (_) {
                                  setModalState(() {
                                    selectedIntervalStart = interval.startMinutes;
                                  });
                                },
                              );
                            }).toList(),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: Text(l10n.translate('cancel')),
                ),
                FilledButton(
                  onPressed: selectedIntervalStart < 0
                      ? null
                      : () {
                          final result = DateTime(
                            selectedDate.year,
                            selectedDate.month,
                            selectedDate.day,
                            selectedIntervalStart ~/ 60,
                            selectedIntervalStart % 60,
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

    final needsSchedule = !partnerOpen || _deliveryMode == _DeliveryMode.schedule;

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

      await ref.read(cartNotifierProvider.notifier).placeOrder(
            promoCode: _appliedPromoCode,
            addressId: _useMapAddress ? null : effectiveSavedAddress?.id.toString(),
            deliveryAddressDetails: deliveryAddressDetails,
            paymentMethod: _paymentMethodCash,
            scheduledDeliveryTime: needsSchedule ? _scheduledDateTime : null,
          );

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            needsSchedule
                ? '${l10n.translate('order_scheduled_for')} ${_formatScheduleDateTime(_scheduledDateTime!)}.'
                : l10n.translate('order_success'),
          ),
          backgroundColor: Colors.green,
        ),
      );
      context.go(RouteNames.orders);
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
    final hasAddress = (_useMapAddress && mapLocation != null) ||
        effectiveSavedAddress != null ||
        mapLocation != null;

    final coords = _resolveCoordinates(
      addresses: addresses,
      mapLocation: mapLocation,
      effectiveSavedAddress: effectiveSavedAddress,
    );

    final outOfZone = _isCurrentPartnerOutOfZone(userLat: coords.lat, userLng: coords.lng);
    final partnerOpen = _partnerInfo?.isOpen ?? true;
    final minimumOrder = _partnerInfo?.minimumOrder ?? 0;
    final minNotReached = minimumOrder > 0 && subtotal < minimumOrder;

    final serviceFee = _computeServiceFee(subtotal);
    final deliveryFee = _effectiveDeliveryFee(subtotal);
    final promoDiscount = _promoDiscount(subtotal);
    final total = (subtotal + deliveryFee + serviceFee - promoDiscount)
        .clamp(0, double.infinity)
        .toDouble();

    final needsSchedule = !partnerOpen || _deliveryMode == _DeliveryMode.schedule;

    final displayedItems = _showAllItems || items.length <= 2 ? items : items.take(2).toList();
    final orderItemsLabel =
      itemCount == 1 ? l10n.translate('item_singular') : l10n.translate('items_plural');

    final canSubmit =
        items.isNotEmpty && !_isPlacingOrder && !minNotReached && !outOfZone && hasAddress;

    return Theme(
      data: lightTheme,
      child: Scaffold(
      appBar: AppBar(
        title: Text(l10n.translate('checkout')),
      ),
      body: items.isEmpty
          ? const _EmptyCheckoutView()
          : ListView(
              padding: EdgeInsets.fromLTRB(
                horizontalPadding,
                14,
                horizontalPadding,
                130 + media.padding.bottom,
              ),
              children: [
                _SectionCard(
                  title: '${l10n.translate('order_section_title')} ($itemCount $orderItemsLabel)',
                  trailing: items.length > 2
                      ? TextButton(
                          onPressed: () {
                            setState(() => _showAllItems = !_showAllItems);
                          },
                          child: Text(
                            _showAllItems
                                ? l10n.translate('collapse')
                                : l10n.translate('show_all'),
                          ),
                        )
                      : null,
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
                                  style: const TextStyle(fontWeight: FontWeight.w700),
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      item.productName,
                                      style: const TextStyle(
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                    if (item.selectedOptions.isNotEmpty)
                                      Text(
                                        item.selectedOptionsDisplay.join(', '),
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
                                style: const TextStyle(fontWeight: FontWeight.w700),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 10),
                _SectionCard(
                  title: l10n.translate('delivery_address'),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (_useMapAddress && mapLocation != null)
                        _AddressPreview(
                          title: mapLocation.customLabel ?? l10n.translate('map_address_title'),
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
                          subtitle: l10n.translate('choose_saved_or_map_address'),
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
                const SizedBox(height: 10),
                _SectionCard(
                  title: l10n.translate('delivery'),
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
                            color: const Color(0xFFFFF4E6),
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: const Color(0xFFF3D1A5)),
                          ),
                          child: Text(
                            l10n.translate('partner_closed_schedule_only'),
                            style: const TextStyle(fontWeight: FontWeight.w700),
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

                            final scheduleOption = RadioListTile<_DeliveryMode>(
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
                              title: Text(l10n.translate('delivery_schedule')),
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
                                style: const TextStyle(fontWeight: FontWeight.w700),
                              ),
                              const SizedBox(height: 8),
                              OutlinedButton.icon(
                                onPressed: () async {
                                  final picked = await _pickScheduledDateTime();
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
                const SizedBox(height: 10),
                _SectionCard(
                  title: l10n.translate('payment_and_promo'),
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
                                style: const TextStyle(fontWeight: FontWeight.w700),
                              ),
                            ),
                            const Icon(Icons.check_circle, color: Colors.green),
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
                              color: _promoMessageIsError ? Colors.red : Colors.green,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 10),
                _SectionCard(
                  title: l10n.translate('final_summary'),
                  child: Column(
                    children: [
                      _SummaryRow(label: l10n.translate('products_label'), value: _money(subtotal)),
                      _SummaryRow(
                        label: l10n.translate('delivery'),
                        value: _isLoadingDeliveryFee
                            ? l10n.translate('loading')
                            : _money(deliveryFee),
                      ),
                      _SummaryRow(label: l10n.translate('service'), value: _money(serviceFee)),
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

class _SectionCard extends StatelessWidget {
  final String title;
  final Widget child;
  final Widget? trailing;

  const _SectionCard({
    required this.title,
    required this.child,
    this.trailing,
  });

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
            Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 16,
                    ),
                  ),
                ),
                if (trailing != null) trailing!,
              ],
            ),
            const SizedBox(height: 8),
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
                Text(
                  subtitle,
                  style: const TextStyle(color: Colors.black54),
                ),
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
            const Icon(Icons.remove_shopping_cart_outlined, size: 52, color: Colors.black38),
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

class _PartnerTimeWindow {
  final int startMinutes;
  final int endMinutes;

  const _PartnerTimeWindow({
    required this.startMinutes,
    required this.endMinutes,
  });
}

class _HalfHourInterval {
  final int startMinutes;
  final int endMinutes;

  const _HalfHourInterval({
    required this.startMinutes,
    required this.endMinutes,
  });
}
