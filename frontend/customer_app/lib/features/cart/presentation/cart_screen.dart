import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/utils/delivery_zone_utils.dart';
import '../../location/presentation/providers/location_provider.dart';
import '../../profile/data/models/address_model.dart';
import '../../profile/presentation/providers/address_provider.dart';
import '../cart_providers.dart';
import 'widgets/cart_item_tile.dart';
import 'widgets/minimum_order_banner.dart';
import 'widgets/order_summary_card.dart';
import 'widgets/partner_closed_banner.dart';
import 'widgets/partner_header.dart';

class CartScreen extends ConsumerStatefulWidget {
  const CartScreen({super.key});

  @override
  ConsumerState<CartScreen> createState() => _CartScreenState();
}

enum _CheckoutAction {
  now,
  schedule,
}

class _CartScreenState extends ConsumerState<CartScreen> {
  static const String _paymentMethodCash = 'CASH';
  static const int _scheduleLeadMinutes = 15;

  String? _loadedPartnerId;
  PartnerCartInfo? _partnerInfo;
  String? _promoCode;
  String? _selectedPaymentMethod;
  double _promoDiscount = 0;
  bool _isPlacingOrder = false;

  ({double lat, double lng}) _resolveCoordinates() {
    final selectedLoc = ref.read(locationNotifierProvider).location;
    if (selectedLoc != null) {
      return (lat: selectedLoc.latitude, lng: selectedLoc.longitude);
    }

    final addresses = ref.read(addressNotifierProvider).valueOrNull ?? [];
    AddressModel? target;

    for (final a in addresses) {
      if (a.isDefault && a.latitude != null && a.longitude != null) {
        target = a;
        break;
      }
    }

    if (target == null) {
      for (final a in addresses) {
        if (a.latitude != null && a.longitude != null) {
          target = a;
          break;
        }
      }
    }

    if (target != null) {
      return (lat: target.latitude!, lng: target.longitude!);
    }

    return (lat: 0.0, lng: 0.0);
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

  void _ensurePartnerLoaded(String? partnerId) {
    if (_loadedPartnerId == partnerId) return;

    _loadedPartnerId = partnerId;
    _partnerInfo = null;
    _promoCode = null;
    _selectedPaymentMethod = null;
    _promoDiscount = 0;

    if (partnerId == null || partnerId.isEmpty) {
      if (mounted) setState(() {});
      return;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final info = await ref
          .read(cartRepositoryProvider)
          .fetchPartnerInfo(partnerId);
      if (!mounted) return;
      setState(() {
        _partnerInfo = info;
      });
    });
  }

  Future<void> _confirmClearCart() async {
    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Vider le panier ?'),
          content: const Text('Êtes-vous sûr de vouloir vider votre panier ?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Annuler'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              style: FilledButton.styleFrom(backgroundColor: Colors.red),
              child: const Text('Vider'),
            ),
          ],
        );
      },
    );

    if (accepted == true) {
      await ref.read(cartNotifierProvider.notifier).clearCart();
    }
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
      if (hour.dayOfWeek.toUpperCase() != dayName || hour.isClosed) {
        continue;
      }

      if (hour.is24Hours) {
        windows.add(
          const _PartnerTimeWindow(startMinutes: 0, endMinutes: 1439),
        );
        continue;
      }

      final open = _parseTimeToMinutes(hour.openTime);
      final close = _parseTimeToMinutes(hour.closeTime);
      if (open == null || close == null) {
        continue;
      }

      if (close > open) {
        windows.add(_PartnerTimeWindow(startMinutes: open, endMinutes: close));
      } else {
        // Overnight slot, keep today's evening segment.
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
      if (open == null || close == null) {
        continue;
      }

      // Overnight slot from previous day contributes to current day early hours.
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
        continue;
      }

      merged.add(window);
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
    final startOfWeek =
        today.subtract(Duration(days: today.weekday - DateTime.monday));

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
          intervals.add(
            _HalfHourInterval(startMinutes: minute, endMinutes: minute + 30),
          );
        }
        minute += 30;
      }
    }

    intervals.sort((a, b) => a.startMinutes.compareTo(b.startMinutes));
    return intervals;
  }

  String _formatScheduleDayLabel(DateTime date) {
    const dayLabels = <String>[
      'Lun',
      'Mar',
      'Mer',
      'Jeu',
      'Ven',
      'Sam',
      'Dim',
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

  String _formatScheduleDateTime(DateTime value) {
    final d = value.day.toString().padLeft(2, '0');
    final m = value.month.toString().padLeft(2, '0');
    final h = value.hour.toString().padLeft(2, '0');
    final min = value.minute.toString().padLeft(2, '0');
    return '$d/$m a $h:$min';
  }

  Future<DateTime?> _pickScheduledDateTime() async {
    final info = _partnerInfo;
    if (info == null || info.openingHours.isEmpty) {
      if (!mounted) return null;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Impossible de charger les horaires du partenaire.'),
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
        const SnackBar(
          content: Text('Aucun creneau disponible cette semaine.'),
          backgroundColor: Colors.red,
        ),
      );
      return null;
    }

    var selectedDate = initialDate;
    var selectedIntervalStart =
      _buildHalfHourIntervalsForDate(selectedDate).first.startMinutes;

    final pickedDateTime = await showDialog<DateTime>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            final media = MediaQuery.of(context);
            final intervals = _buildHalfHourIntervalsForDate(selectedDate);
            if (!intervals.any((e) => e.startMinutes == selectedIntervalStart)) {
              selectedIntervalStart = intervals.isEmpty
                  ? -1
                  : intervals.first.startMinutes;
            }

            return AlertDialog(
              title: const Text('Planifier la commande'),
              content: SizedBox(
                width: double.maxFinite,
                child: ConstrainedBox(
                  constraints: BoxConstraints(
                    maxHeight: media.size.height * 0.62,
                  ),
                  child: SingleChildScrollView(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Semaine courante'),
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
                              final hasSlots = _buildHalfHourIntervalsForDate(date)
                                  .isNotEmpty;

                              return ChoiceChip(
                                label: Text(_formatScheduleDayLabel(date)),
                                selected: isSelected,
                                onSelected: (_) {
                                  setModalState(() {
                                    selectedDate = date;
                                    final nextIntervals =
                                        _buildHalfHourIntervalsForDate(date);
                                    selectedIntervalStart = nextIntervals.isEmpty
                                        ? -1
                                        : nextIntervals.first.startMinutes;
                                  });
                                },
                                side: BorderSide(
                                  color: hasSlots
                                      ? Colors.black26
                                      : Colors.black12,
                                ),
                              );
                            },
                          ),
                        ),
                        const SizedBox(height: 12),
                        if (intervals.isEmpty)
                          const Text(
                            'Aucun creneau disponible pour ce jour.',
                            style: TextStyle(color: Colors.black54),
                          )
                        else
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: intervals.map((interval) {
                              final isSelected =
                                  interval.startMinutes == selectedIntervalStart;
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
                  child: const Text('Annuler'),
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
                  child: const Text('Valider'),
                ),
              ],
            );
          },
        );
      },
    );

    return pickedDateTime;
  }

  Future<void> _planAndPlaceOrder() async {
    final scheduledAt = await _pickScheduledDateTime();
    if (scheduledAt == null) return;
    await _placeOrder(scheduledDeliveryTime: scheduledAt);
  }

  Future<void> _handleCheckoutPress({required bool isOpen}) async {
    if (!isOpen) {
      await _planAndPlaceOrder();
      return;
    }

    if (!mounted) return;

    final action = await showModalBottomSheet<_CheckoutAction>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ListTile(
                leading: const Icon(Icons.flash_on_rounded),
                title: const Text('Commander maintenant'),
                subtitle: const Text('Validation immediate de la commande.'),
                onTap: () => Navigator.of(sheetContext).pop(_CheckoutAction.now),
              ),
              ListTile(
                leading: const Icon(Icons.schedule),
                title: const Text('Planifier la commande'),
                subtitle: const Text('Choisir un jour et un creneau de 30 min.'),
                onTap: () =>
                    Navigator.of(sheetContext).pop(_CheckoutAction.schedule),
              ),
            ],
          ),
        );
      },
    );

    if (action == _CheckoutAction.now) {
      await _placeOrder();
      return;
    }

    if (action == _CheckoutAction.schedule) {
      await _planAndPlaceOrder();
    }
  }

  Future<void> _placeOrder({DateTime? scheduledDeliveryTime}) async {
    final selectedPaymentMethod = _selectedPaymentMethod;
    if (selectedPaymentMethod == null) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez choisir un mode de paiement.'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    final coords = _resolveCoordinates();
    final zoneUserLat = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lat;
    final zoneUserLng = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lng;
    final outOfZone = _isCurrentPartnerOutOfZone(
      userLat: zoneUserLat,
      userLng: zoneUserLng,
    );
    if (outOfZone) {
      if (!mounted) return;
      final l10n = AppLocalizations.of(context);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('partner_details_out_of_zone')),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(() => _isPlacingOrder = true);
    try {
      await ref
          .read(cartNotifierProvider.notifier)
          .placeOrder(
            promoCode: _promoCode,
            paymentMethod: selectedPaymentMethod,
            scheduledDeliveryTime: scheduledDeliveryTime,
          );

      if (!mounted) return;
      final successText = scheduledDeliveryTime != null
          ? 'Commande planifiee pour ${_formatScheduleDateTime(scheduledDeliveryTime)}.'
          : 'Commande validee avec succes.';
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(successText),
          backgroundColor: Colors.green,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Échec de la commande: $e'),
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
    final l10n = AppLocalizations.of(context);
    final media = MediaQuery.of(context);
    final screenWidth = media.size.width;
    final horizontalPadding = screenWidth < 360 ? 10.0 : 14.0;
    final bodyBottomPadding = (screenWidth < 360 ? 118.0 : 110.0) +
        media.padding.bottom;
    final checkoutButtonHeight = screenWidth < 360 ? 48.0 : 52.0;

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

    ref.watch(addressNotifierProvider);
    ref.watch(locationNotifierProvider);

    final cartState = ref.watch(cartNotifierProvider);
    final items = cartState.items;
    final subtotal = cartState.subtotal;
    final partnerId = items.isNotEmpty ? items.first.partnerId : null;

    final coords = _resolveCoordinates();
    final zoneUserLat = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lat;
    final zoneUserLng = (coords.lat == 0.0 && coords.lng == 0.0)
        ? null
        : coords.lng;

    _ensurePartnerLoaded(partnerId);

    final fallbackPartnerName = items.isNotEmpty ? items.first.partnerName : '';
    final fallbackPartnerLogo = items.isNotEmpty
        ? items.first.partnerLogoUrl
        : '';

    final partnerName = _partnerInfo?.partnerName ?? fallbackPartnerName;
    final partnerLogoUrl = _partnerInfo?.partnerLogoUrl ?? fallbackPartnerLogo;
    final minimumOrder = _partnerInfo?.minimumOrder ?? 0;
    final isOpen = _partnerInfo?.isOpen ?? true;
    final outOfZone = _isCurrentPartnerOutOfZone(
      userLat: zoneUserLat,
      userLng: zoneUserLng,
    );
    final serviceFee = _computeServiceFee(subtotal);

    final minNotReached = minimumOrder > 0 && subtotal < minimumOrder;
    final missingAmount = (minimumOrder - subtotal)
        .clamp(0, double.infinity)
        .toDouble();
    final hasSelectedPaymentMethod = _selectedPaymentMethod != null;
    final partnerLoaded = _partnerInfo != null || partnerId == null;

    final canProceed =
        items.isNotEmpty &&
        !minNotReached &&
        !outOfZone &&
        hasSelectedPaymentMethod &&
      partnerLoaded &&
        !_isPlacingOrder;

    return Theme(
      data: lightTheme,
      child: Scaffold(
      appBar: AppBar(
        title: const Text('Mon panier'),
        actions: [
          if (items.isNotEmpty)
            TextButton(
              onPressed: _confirmClearCart,
              child: const Text(
                'Vider',
                style: TextStyle(
                  color: Colors.red,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
        ],
      ),
      body: items.isEmpty
          ? const _EmptyCartView()
          : ListView(
              padding: EdgeInsets.fromLTRB(
                horizontalPadding,
                14,
                horizontalPadding,
                bodyBottomPadding,
              ),
              children: [
                PartnerHeader(
                  partnerName: partnerName,
                  partnerLogoUrl: partnerLogoUrl,
                ),
                if (!isOpen) const PartnerClosedBanner(),
                if (!isOpen)
                  Container(
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF7E6),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: const Color(0xFFFCD9A6)),
                    ),
                    child: const Row(
                      children: [
                        Icon(
                          Icons.schedule,
                          size: 18,
                          color: Color(0xFFB96500),
                        ),
                        SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Vous pouvez planifier la commande selon les horaires d ouverture.',
                            style: TextStyle(
                              color: Color(0xFF7C4A00),
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                if (outOfZone)
                  Container(
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF2F2),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: const Color(0xFFFFD7D7)),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.location_off_outlined,
                          size: 18,
                          color: Colors.red,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            '${l10n.translate('partner_details_out_of_zone')}.',
                            style: const TextStyle(
                              color: Colors.red,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                if (minNotReached)
                  MinimumOrderBanner(
                    minimumOrder: minimumOrder,
                    missingAmount: missingAmount,
                  ),
                ...items.map(
                  (item) => CartItemTile(
                    item: item,
                    onQuantityChanged: (newQty) {
                      ref
                          .read(cartNotifierProvider.notifier)
                          .updateQuantity(
                            itemKey: item.uniqueKey,
                            quantity: newQty,
                          );
                    },
                    onRemove: () {
                      ref
                          .read(cartNotifierProvider.notifier)
                          .removeItem(item.uniqueKey);
                    },
                    onCustomizationChanged: (options, note) {
                      ref
                          .read(cartNotifierProvider.notifier)
                          .updateItemCustomization(
                            itemKey: item.uniqueKey,
                            selectedOptions: options,
                            kitchenNote: note,
                          );
                    },
                  ),
                ),
                OrderSummaryCard(
                  subtotal: subtotal,
                  partnerId: partnerId,
                  serviceFee: serviceFee,
                  discount: _promoDiscount,
                  promoCode: _promoCode,
                  onDeliveryFeeChanged: (_) {},
                  onPromoChanged: (promoCode, discount) {
                    setState(() {
                      _promoCode = promoCode;
                      _promoDiscount = discount;
                    });
                  },
                ),
                const SizedBox(height: 10),
                _PaymentMethodCard(
                  selectedMethod: _selectedPaymentMethod,
                  onMethodChanged: (method) {
                    setState(() {
                      _selectedPaymentMethod = method;
                    });
                  },
                ),
              ],
            ),
      bottomNavigationBar: items.isEmpty
          ? null
          : SafeArea(
              minimum: EdgeInsets.fromLTRB(
                horizontalPadding,
                8,
                horizontalPadding,
                10,
              ),
              child: SizedBox(
                height: checkoutButtonHeight,
                child: ElevatedButton(
                  onPressed: canProceed
                      ? () => _handleCheckoutPress(isOpen: isOpen)
                      : null,
                  child: FittedBox(
                    fit: BoxFit.scaleDown,
                    child: Text(
                      canProceed
                          ? (isOpen
                                ? 'Commander ou planifier'
                                : 'Planifier la commande')
                        : (!partnerLoaded
                          ? 'Chargement...'
                          : outOfZone
                                ? l10n.translate('delivery_out_of_zone_badge')
                                : !hasSelectedPaymentMethod
                                ? 'Choisir le paiement'
                                : minNotReached
                                ? 'Minimum non atteint'
                                : 'Panier vide'),
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ),
                ),
              ),
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

  bool contains(int minute) {
    return minute >= startMinutes && minute <= endMinutes;
  }
}

class _HalfHourInterval {
  final int startMinutes;
  final int endMinutes;

  const _HalfHourInterval({
    required this.startMinutes,
    required this.endMinutes,
  });
}

class _PaymentMethodCard extends StatelessWidget {
  final String? selectedMethod;
  final ValueChanged<String?> onMethodChanged;

  const _PaymentMethodCard({
    required this.selectedMethod,
    required this.onMethodChanged,
  });

  @override
  Widget build(BuildContext context) {
    final isCashSelected =
        selectedMethod == _CartScreenState._paymentMethodCash;

    return Card(
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Mode de paiement',
              style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
            ),
            const SizedBox(height: 6),
            const Text(
              'Veuillez choisir un mode de paiement avant validation.',
              style: TextStyle(color: Colors.black54),
            ),
            const SizedBox(height: 8),
            InkWell(
              borderRadius: BorderRadius.circular(12),
              onTap: () => onMethodChanged(_CartScreenState._paymentMethodCash),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: isCashSelected
                        ? Theme.of(context).colorScheme.primary
                        : Colors.black12,
                    width: isCashSelected ? 1.6 : 1,
                  ),
                  color: isCashSelected
                      ? Theme.of(
                          context,
                        ).colorScheme.primary.withValues(alpha: 0.07)
                      : Colors.transparent,
                ),
                child: Row(
                  children: [
                    const Icon(Icons.payments_outlined),
                    const SizedBox(width: 10),
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Paiement à la livraison',
                            style: TextStyle(fontWeight: FontWeight.w600),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'Règlement en espèces à la réception.',
                            style: TextStyle(color: Colors.black54),
                          ),
                        ],
                      ),
                    ),
                    Icon(
                      isCashSelected
                          ? Icons.check_circle
                          : Icons.radio_button_unchecked,
                      color: isCashSelected
                          ? Theme.of(context).colorScheme.primary
                          : Colors.black45,
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyCartView extends StatelessWidget {
  const _EmptyCartView();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Padding(
        padding: EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.shopping_bag_outlined, size: 56, color: Colors.black38),
            SizedBox(height: 8),
            Text(
              'Votre panier est vide',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            SizedBox(height: 6),
            Text(
              'Ajoutez des articles depuis un partenaire pour commencer.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.black54),
            ),
          ],
        ),
      ),
    );
  }
}
