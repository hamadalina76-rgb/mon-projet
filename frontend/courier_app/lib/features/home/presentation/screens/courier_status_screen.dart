import 'dart:async';
import 'dart:math' show max;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../config/di/injection_container.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../providers/current_courier_provider.dart';
import '../../../../providers/tracking_provider.dart';
import '../../../../services/notification_service.dart';
import '../../../auth/domain/repositories/auth_repository.dart';

// ─────────────────────────────────────────────────────────────────────────────
// CONSTANTS
// ─────────────────────────────────────────────────────────────────────────────

const _kRed = Color(0xFFBF2B2B);
const _kRedDark = Color(0xFF9B1F1F);

const _kDays = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

const _kMonths = [
  '',
  'Jan',
  'Fév',
  'Mar',
  'Avr',
  'Mai',
  'Juin',
  'Juil',
  'Août',
  'Sep',
  'Oct',
  'Nov',
  'Déc',
];

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

String _normalizeTime(String value) {
  final v = value.trim();
  return v.length >= 5 ? v.substring(0, 5) : v;
}

double? _parseHourValue(String value) {
  final raw = value.trim();
  if (raw.isEmpty) return null;
  final normalized = raw.length >= 5 ? raw.substring(0, 5) : raw;
  final parts = normalized.split(':');
  if (parts.length != 2) return null;
  final h = int.tryParse(parts[0]);
  final m = int.tryParse(parts[1]);
  if (h == null || m == null) return null;
  return h + (m / 60.0);
}

String _readFirstNonEmpty(Map<String, dynamic> source, List<String> keys) {
  for (final key in keys) {
    final value = source[key];
    if (value == null) continue;
    final text = value.toString().trim();
    if (text.isNotEmpty && text.toLowerCase() != 'null') return text;
  }
  return '';
}

double? _parseExceptionHour(Map<String, dynamic> exception, List<String> candidateKeys) {
  final raw = _readFirstNonEmpty(exception, candidateKeys);
  if (raw.isEmpty) return null;

  // ISO date-time: 2026-04-01T09:00:00
  if (raw.contains('T') && raw.length >= 16) {
    return _parseHourValue(raw.substring(11, 16));
  }

  // Time only: 09:00 or 09:00:00
  if (raw.length >= 5 && raw[2] == ':') {
    return _parseHourValue(raw.substring(0, 5));
  }

  // Last fallback: parse as DateTime then extract local hour/minute.
  final dt = DateTime.tryParse(raw);
  if (dt == null) return null;
  return dt.toLocal().hour + (dt.toLocal().minute / 60.0);
}

bool _hasExceptionTimeRange(Map<String, dynamic>? exception) {
  if (exception == null) return false;
  final start = _parseExceptionHour(exception, ['startsAt', 'startAt', 'starts_at', 'startTime', 'start_time']);
  final end = _parseExceptionHour(exception, ['endsAt', 'endAt', 'ends_at', 'endTime', 'end_time']);
  return start != null && end != null && end > start;
}

/// Returns the Monday of the week containing [date].
DateTime _weekMonday(DateTime date) {
  // In Dart, Monday is 1 and Sunday is 7.
  // We want to subtract (weekday - 1) days to get to Monday.
  return DateTime(date.year, date.month, date.day)
      .subtract(Duration(days: date.weekday - 1));
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────

class CourierStatusScreen extends ConsumerStatefulWidget {
  const CourierStatusScreen({super.key});

  @override
  ConsumerState<CourierStatusScreen> createState() => _CourierStatusScreenState();
}

class _CourierStatusScreenState extends ConsumerState<CourierStatusScreen> {
  final _durationController = TextEditingController();
  final _commentController = TextEditingController();

  String _selectedReason = 'PANNE';
  bool _submitting = false;
  bool _reportingProblem = false;
  bool _markingAvailable = false;
  bool _loadingHistory = false;
  bool _historyInitialized = false;
  bool _loadingInternalSchedule = false;
  bool _internalScheduleInitialized = false;
  String _historyState = 'ALL';
  List<Map<String, dynamic>> _history = const [];

  // Week shown in the calendar (starts on Monday).
  // _effectiveWeek: list of 7 EffectiveScheduleResponse maps (Mon→Sun),
  // already merged with admin exceptions by the backend.
  late DateTime _calendarWeekStart;
  List<Map<String, dynamic>> _effectiveWeek = const [];

  // Periodic timer — refreshes effective week every 30 s for real-time sync
  Timer? _exceptionsRefreshTimer;
  StreamSubscription? _notifSubscription;

  bool _isInternalCourier(dynamic courier) {
    final type = (courier?.courierType ?? '').toString().toUpperCase();
    return type == 'INTERNAL';
  }

  String _fmtIsoDate(DateTime d) {
    return '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
  }

  bool _dateWithin(String date, String start, String end) {
    if (date.isEmpty || start.isEmpty || end.isEmpty) return false;
    return date.compareTo(start) >= 0 && date.compareTo(end) <= 0;
  }

  Future<List<Map<String, dynamic>>> _enrichWeekExceptionsWithTime(
    List<Map<String, dynamic>> week,
  ) async {
    if (week.isEmpty || !getIt.isRegistered<AuthRepository>()) return week;

    final needsEnrichment = week.any((day) {
      final ex = day['exception'];
      if (ex is! Map<String, dynamic>) return false;
      final hasStart = _readFirstNonEmpty(ex, ['startsAt', 'startAt', 'starts_at', 'startTime', 'start_time']).isNotEmpty;
      final hasEnd = _readFirstNonEmpty(ex, ['endsAt', 'endAt', 'ends_at', 'endTime', 'end_time']).isNotEmpty;
      return !(hasStart && hasEnd);
    });

    if (!needsEnrichment) return week;

    final from = _fmtIsoDate(_calendarWeekStart);
    final to = _fmtIsoDate(_calendarWeekStart.add(const Duration(days: 6)));
    final detailed = await getIt<AuthRepository>().getMyExceptionalSchedules(from: from, to: to);
    if (detailed.isEmpty) return week;

    return week.map((day) {
      final dayCopy = Map<String, dynamic>.from(day);
      final ex = dayCopy['exception'];
      if (ex is! Map<String, dynamic>) return dayCopy;

      final exCopy = Map<String, dynamic>.from(ex);
      Map<String, dynamic>? match;

      final exId = exCopy['id']?.toString();
      if (exId != null && exId.isNotEmpty) {
        for (final d in detailed) {
          if (d['id']?.toString() == exId) {
            match = d;
            break;
          }
        }
      }

      if (match == null) {
        final dayDate = (dayCopy['date'] ?? '').toString();
        final exType = (exCopy['exceptionType'] ?? '').toString();
        for (final d in detailed) {
          final start = (d['startDate'] ?? '').toString();
          final end = (d['endDate'] ?? '').toString();
          final type = (d['exceptionType'] ?? '').toString();
          if (_dateWithin(dayDate, start, end) && (exType.isEmpty || exType == type)) {
            match = d;
            break;
          }
        }
      }

      if (match != null) {
        exCopy['startsAt'] = _readFirstNonEmpty(match, ['startsAt', 'startAt', 'starts_at', 'startTime', 'start_time']);
        exCopy['endsAt'] = _readFirstNonEmpty(match, ['endsAt', 'endAt', 'ends_at', 'endTime', 'end_time']);
      }

      dayCopy['exception'] = exCopy;
      return dayCopy;
    }).toList();
  }

  @override
  void initState() {
    super.initState();
    _calendarWeekStart = _weekMonday(DateTime.now());
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _ensureDataLoaded();
    });
    // Real-time sync: refresh every 30 s so admin changes appear immediately
    _exceptionsRefreshTimer = Timer.periodic(const Duration(seconds: 30), (_) {
      if (!mounted) return;
      final courier = ref.read(currentCourierProvider).asData?.value;
      if (_isInternalCourier(courier)) {
        _loadEffectiveWeek();
      }
    });

    // Listen to profile refreshes from NotificationService
    _notifSubscription = NotificationService().onProfileRefreshed.listen((_) {
      if (mounted) {
        final courier = ref.read(currentCourierProvider).asData?.value;
        if (_isInternalCourier(courier)) {
          _loadEffectiveWeek();
        }
        _loadHistory();
      }
    });
  }

  @override
  void dispose() {
    _exceptionsRefreshTimer?.cancel();
    _notifSubscription?.cancel();
    _durationController.dispose();
    _commentController.dispose();
    super.dispose();
  }

  // ── Data loaders ────────────────────────────────────────────────────────

  Future<void> _loadHistory() async {
    if (!getIt.isRegistered<AuthRepository>()) return;
    setState(() => _loadingHistory = true);
    try {
      final history = await getIt<AuthRepository>().getMyUnavailabilityDeclarations(
        state: _historyStateQuery,
      );
      if (!mounted) return;
      setState(() => _history = history);
    } catch (_) {
      if (!mounted) return;
      setState(() => _history = const []);
    } finally {
      if (mounted) {
        setState(() {
          _loadingHistory = false;
          _historyInitialized = true;
        });
      }
    }
  }

  String? get _historyStateQuery {
    return switch (_historyState) {
      'SUBMITTED' => 'submitted',
      'IN_PROGRESS' => 'en_cours',
      'REJECTED' => 'rejected',
      'RESOLVED' => 'resolved',
      _ => null,
    };
  }

  /// Loads the 7-day effective schedule (fixed + exceptions merged) from backend.
  Future<void> _loadEffectiveWeek() async {
    if (!getIt.isRegistered<AuthRepository>()) return;
    setState(() => _loadingInternalSchedule = true);
    try {
      final week = await getIt<AuthRepository>().getMyEffectiveWeek(
        from: _fmtIsoDate(_calendarWeekStart),
      );
      final enrichedWeek = await _enrichWeekExceptionsWithTime(week);
      if (!mounted) return;
      
      // LOG pour debug (à retirer en prod si nécessaire)
      print('DEBUG: Effective week loaded: ${enrichedWeek.length} days');
      for (var day in enrichedWeek) {
        print('Day ${day['date']}: ${day['shifts']?.length} shifts, status=${day['status']}');
      }

      setState(() => _effectiveWeek = enrichedWeek);
    } catch (e) {
      print('ERROR loading effective week: $e');
      if (!mounted) return;
      setState(() => _effectiveWeek = const []);
    } finally {
      if (mounted) {
        setState(() {
          _loadingInternalSchedule = false;
          _internalScheduleInitialized = true;
        });
      }
    }
  }

  Future<void> _navigateCalendarWeek(int delta) async {
    setState(() {
      _calendarWeekStart = _calendarWeekStart.add(Duration(days: 7 * delta));
      _effectiveWeek = const [];
    });
    await _loadEffectiveWeek();
  }

  Future<void> _updateHistoryState(String state) async {
    if (_historyState == state) return;
    setState(() => _historyState = state);
    await _loadHistory();
  }

  // ── Actions ─────────────────────────────────────────────────────────────

  Future<void> _submitDeclaration() async {
    final l10n = AppLocalizations.of(context)!;
    if (!getIt.isRegistered<AuthRepository>()) return;
    setState(() => _submitting = true);
    try {
      final duration = int.tryParse(_durationController.text.trim());
      final startsAt = DateTime.now();
      final endsAt = duration == null ? null : startsAt.add(Duration(minutes: duration));
      final response = await getIt<AuthRepository>().declareUnavailability(
        reason: _selectedReason,
        estimatedDurationMinutes: duration,
        comment: _commentController.text.trim().isEmpty
            ? null
            : _commentController.text.trim(),
        startsAt: startsAt,
        endsAt: endsAt,
      );

      ref.invalidate(currentCourierProvider);

      final validationStatus =
          (response['validationStatus'] ?? '').toString().toUpperCase();

      if (validationStatus == 'APPROVED_ACTIVE' && mounted) {
        await ref
            .read(trackingProvider.notifier)
            .setOnline(context, false, inDelivery: false);
      }

      if (!mounted) return;

      _showSnackBar(
        validationStatus == 'PENDING_VALIDATION'
            ? l10n.translate('status_pending_validation_message')
            : l10n.translate('status_applied_message'),
      );

      _durationController.clear();
      _commentController.clear();
      await _loadHistory();
    } catch (e) {
      if (!mounted) return;
      _showSnackBar('${l10n.translate('generic_try_again_error')}: $e');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _reportProblemToAdmin({
    required String reason,
    int? estimatedDurationMinutes,
    String? comment,
    DateTime? startsAt,
    DateTime? endsAt,
  }) async {
    final l10n = AppLocalizations.of(context)!;
    if (!getIt.isRegistered<AuthRepository>()) return;

    if (!mounted) return;
    setState(() => _reportingProblem = true);
    try {
      await getIt<AuthRepository>().declareUnavailability(
        reason: reason,
        estimatedDurationMinutes: estimatedDurationMinutes,
        comment: comment,
        startsAt: startsAt,
        endsAt: endsAt,
      );

      ref.invalidate(currentCourierProvider);
      await _loadHistory();
      if (!mounted) return;
      _showSnackBar(l10n.translate('status_problem_sent_admin'));
    } catch (e) {
      if (!mounted) return;
      _showSnackBar('${l10n.translate('generic_try_again_error')}: $e');
    } finally {
      if (mounted) setState(() => _reportingProblem = false);
    }
  }

  Future<void> _openInternalProblemDialog() async {
    final l10n = AppLocalizations.of(context)!;
    String reason = 'PANNE';
    String commentInput = '';
    DateTime startDate = DateTime.now();
    DateTime? endDate;
    TimeOfDay startTime = TimeOfDay.now();
    TimeOfDay? endTime;
    String selectedDurationPreset = 'custom';
    bool isMultiDay = false;

    TimeOfDay addMinutes(TimeOfDay t, int minutes) {
      final totalMinutes = t.hour * 60 + t.minute + minutes;
      return TimeOfDay(hour: (totalMinutes ~/ 60) % 24, minute: totalMinutes % 60);
    }

    String fmtDate(DateTime d) =>
        '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

    String fmtTime(TimeOfDay t) =>
        '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';

    void applyPreset(String preset, void Function(void Function()) setState) {
      setState(() {
        selectedDurationPreset = preset;
        isMultiDay = false;
        switch (preset) {
          case '4h':
            endTime = addMinutes(startTime, 240);
            endDate = null;
            break;
          case 'half_day':
            if (startTime.hour < 12) {
              startTime = const TimeOfDay(hour: 8, minute: 0);
              endTime = const TimeOfDay(hour: 12, minute: 0);
            } else {
              startTime = const TimeOfDay(hour: 12, minute: 0);
              endTime = const TimeOfDay(hour: 18, minute: 0);
            }
            endDate = null;
            break;
          case 'full_day':
            startTime = const TimeOfDay(hour: 8, minute: 0);
            endTime = const TimeOfDay(hour: 18, minute: 0);
            endDate = null;
            break;
          case 'multi_day':
            isMultiDay = true;
            endDate ??= startDate.add(const Duration(days: 1));
            endTime = null;
            break;
          case 'custom':
            endTime = null;
            endDate = null;
            break;
        }
      });
    }

    final shouldSend = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setDialogState) {
            final theme = Theme.of(ctx);
            final primary = theme.colorScheme.primary;

            Widget chip(String key, String label, IconData? icon) {
              final sel = selectedDurationPreset == key;
              return GestureDetector(
                onTap: () => applyPreset(key, setDialogState),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
                  decoration: BoxDecoration(
                    color: sel ? primary : Colors.grey.shade50,
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(
                      color: sel ? primary : Colors.grey.shade300,
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (icon != null) ...[
                        Icon(icon, size: 15, color: sel ? Colors.white : Colors.grey.shade600),
                        const SizedBox(width: 5),
                      ],
                      Text(
                        label,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: sel ? Colors.white : Colors.grey.shade700,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }

            Widget sectionLabel(String text) => Padding(
              padding: const EdgeInsets.only(bottom: 8, top: 4),
              child: Text(text, style: TextStyle(
                fontSize: 12, fontWeight: FontWeight.w600,
                color: Colors.grey.shade500, letterSpacing: 0.5,
              )),
            );

            Widget pickerBtn({
              required IconData icon,
              required String label,
              required VoidCallback onTap,
            }) {
              return Expanded(
                child: Material(
                  color: Colors.grey.shade50,
                  borderRadius: BorderRadius.circular(12),
                  child: InkWell(
                    onTap: onTap,
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: Colors.grey.shade200),
                      ),
                      child: Row(
                        children: [
                          Icon(icon, size: 18, color: primary),
                          const SizedBox(width: 8),
                          Flexible(
                            child: Text(label, style: const TextStyle(
                              fontSize: 14, fontWeight: FontWeight.w500,
                            ), overflow: TextOverflow.ellipsis),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            }

            return Container(
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
              ),
              padding: EdgeInsets.only(
                left: 20, right: 20, top: 16,
                bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Handle bar
                    Center(
                      child: Container(
                        width: 40, height: 4,
                        margin: const EdgeInsets.only(bottom: 16),
                        decoration: BoxDecoration(
                          color: Colors.grey.shade300,
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                    ),

                    // Title
                    Text(
                      l10n.translate('status_problem_dialog_title'),
                      style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
                    ),
                    const SizedBox(height: 20),

                    // Reason dropdown
                    sectionLabel(l10n.translate('status_reason_label').toUpperCase()),
                    DropdownButtonFormField<String>(
                      initialValue: reason,
                      decoration: InputDecoration(
                        filled: true,
                        fillColor: Colors.grey.shade50,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide(color: Colors.grey.shade200),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide(color: Colors.grey.shade200),
                        ),
                        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                      ),
                      items: [
                        DropdownMenuItem(value: 'PANNE', child: Text(l10n.translate('status_reason_panne'))),
                        DropdownMenuItem(value: 'CONGE', child: Text(l10n.translate('status_reason_conge'))),
                        DropdownMenuItem(value: 'ABSENT', child: Text(l10n.translate('status_reason_absent'))),
                        DropdownMenuItem(value: 'RETARD', child: Text(l10n.translate('status_reason_retard'))),
                        DropdownMenuItem(value: 'NE_TRAVAILLE_PAS', child: Text(l10n.translate('status_reason_not_working'))),
                      ],
                      onChanged: (v) {
                        if (v == null) return;
                        setDialogState(() {
                          reason = v;
                          // Pour CONGE, activer multi-jour par défaut
                          if (v == 'CONGE' && selectedDurationPreset != 'multi_day') {
                            applyPreset('multi_day', setDialogState);
                          }
                        });
                      },
                    ),
                    const SizedBox(height: 16),

                    // Duration presets
                    sectionLabel(l10n.translate('status_duration_label').toUpperCase()),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        chip('4h', l10n.translate('status_duration_4h'), Icons.timer_outlined),
                        chip('half_day', l10n.translate('status_duration_half_day'), Icons.wb_twilight_rounded),
                        chip('full_day', l10n.translate('status_duration_full_day'), Icons.wb_sunny_rounded),
                        chip('multi_day', l10n.translate('status_duration_multi_day'), Icons.date_range_rounded),
                        chip('custom', l10n.translate('status_duration_custom'), Icons.tune_rounded),
                      ],
                    ),
                    const SizedBox(height: 16),

                    // Date(s)
                    sectionLabel(isMultiDay
                        ? l10n.translate('status_date_range_label').toUpperCase()
                        : l10n.translate('status_date_label').toUpperCase()),
                    Row(
                      children: [
                        pickerBtn(
                          icon: Icons.calendar_today_rounded,
                          label: fmtDate(startDate),
                          onTap: () async {
                            final picked = await showDatePicker(
                              context: ctx,
                              initialDate: startDate,
                              firstDate: DateTime.now().subtract(const Duration(days: 1)),
                              lastDate: DateTime.now().add(const Duration(days: 365)),
                            );
                            if (picked != null) {
                              setDialogState(() {
                                startDate = picked;
                                if (endDate != null && endDate!.isBefore(startDate)) {
                                  endDate = startDate.add(const Duration(days: 1));
                                }
                              });
                            }
                          },
                        ),
                        if (isMultiDay) ...[
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 8),
                            child: Icon(Icons.arrow_forward_rounded, size: 18, color: Colors.grey.shade400),
                          ),
                          pickerBtn(
                            icon: Icons.calendar_today_rounded,
                            label: endDate != null ? fmtDate(endDate!) : '—',
                            onTap: () async {
                              final picked = await showDatePicker(
                                context: ctx,
                                initialDate: endDate ?? startDate.add(const Duration(days: 1)),
                                firstDate: startDate,
                                lastDate: DateTime.now().add(const Duration(days: 365)),
                              );
                              if (picked != null) {
                                setDialogState(() => endDate = picked);
                              }
                            },
                          ),
                        ],
                      ],
                    ),

                    // Time pickers (not for multi-day)
                    if (!isMultiDay) ...[
                      const SizedBox(height: 16),
                      sectionLabel(l10n.translate('status_time_label').toUpperCase()),
                      Row(
                        children: [
                          pickerBtn(
                            icon: Icons.access_time_rounded,
                            label: '${l10n.translate('status_from')} ${fmtTime(startTime)}',
                            onTap: () async {
                              final picked = await showTimePicker(context: ctx, initialTime: startTime);
                              if (picked != null) {
                                setDialogState(() {
                                  startTime = picked;
                                  selectedDurationPreset = 'custom';
                                });
                              }
                            },
                          ),
                          const SizedBox(width: 10),
                          pickerBtn(
                            icon: Icons.access_time_rounded,
                            label: endTime != null
                                ? '${l10n.translate('status_to')} ${fmtTime(endTime!)}'
                                : l10n.translate('status_end_time'),
                            onTap: () async {
                              final picked = await showTimePicker(
                                context: ctx,
                                initialTime: endTime ?? addMinutes(startTime, 240),
                              );
                              if (picked != null) {
                                setDialogState(() {
                                  endTime = picked;
                                  selectedDurationPreset = 'custom';
                                });
                              }
                            },
                          ),
                        ],
                      ),
                    ],

                    const SizedBox(height: 16),

                    // Comment
                    sectionLabel(l10n.translate('status_comment_label').toUpperCase()),
                    TextField(
                      maxLines: 2,
                      onChanged: (v) => commentInput = v,
                      decoration: InputDecoration(
                        hintText: l10n.translate('status_comment_hint'),
                        filled: true,
                        fillColor: Colors.grey.shade50,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide(color: Colors.grey.shade200),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide(color: Colors.grey.shade200),
                        ),
                        contentPadding: const EdgeInsets.all(14),
                      ),
                    ),
                    const SizedBox(height: 20),

                    // Buttons
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton(
                            onPressed: () => Navigator.of(ctx).pop(false),
                            style: OutlinedButton.styleFrom(
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              side: BorderSide(color: Colors.grey.shade300),
                            ),
                            child: Text(l10n.translate('cancel'),
                              style: TextStyle(color: Colors.grey.shade600, fontWeight: FontWeight.w600)),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          flex: 2,
                          child: FilledButton.icon(
                            onPressed: () => Navigator.of(ctx).pop(true),
                            icon: const Icon(Icons.send_rounded, size: 18),
                            label: Text(l10n.translate('status_problem_dialog_submit')),
                            style: FilledButton.styleFrom(
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );

    if (!mounted) return;

    final comment = commentInput.trim();

    if (shouldSend == true) {
      if (isMultiDay && endDate != null) {
        // Congé multi-jours — pas d'heures
        final startsAt = DateTime(startDate.year, startDate.month, startDate.day, 0, 0);
        final endsAt = DateTime(endDate!.year, endDate!.month, endDate!.day, 23, 59);
        final durationMinutes = endsAt.difference(startsAt).inMinutes;
        await _reportProblemToAdmin(
          reason: reason,
          estimatedDurationMinutes: durationMinutes > 0 ? durationMinutes : null,
          comment: comment.isEmpty ? null : comment,
          startsAt: startsAt,
          endsAt: endsAt,
        );
      } else {
        // Exception horaire
        final startsAt = DateTime(
          startDate.year, startDate.month, startDate.day,
          startTime.hour, startTime.minute,
        );
        DateTime? endsAt;
        int? durationMinutes;
        if (endTime != null) {
          endsAt = DateTime(
            startDate.year, startDate.month, startDate.day,
            endTime!.hour, endTime!.minute,
          );
          durationMinutes = endsAt.difference(startsAt).inMinutes;
          if (durationMinutes <= 0) durationMinutes = null;
        }
        await _reportProblemToAdmin(
          reason: reason,
          estimatedDurationMinutes: durationMinutes,
          comment: comment.isEmpty ? null : comment,
          startsAt: startsAt,
          endsAt: endsAt,
        );
      }
    }
  }

  Future<void> _markAvailableNow() async {
    final l10n = AppLocalizations.of(context)!;
    if (!getIt.isRegistered<AuthRepository>()) return;
    final courier = ref.read(currentCourierProvider).asData?.value;
    final isInternal = (courier?.courierType ?? '').toUpperCase() == 'INTERNAL';
    if (isInternal) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('status_internal_readonly_info'))),
      );
      return;
    }

    setState(() => _markingAvailable = true);
    try {
      await getIt<AuthRepository>().markAsAvailableNow();
      ref.invalidate(currentCourierProvider);
      if (!mounted) return;
      await ref
          .read(trackingProvider.notifier)
          .setOnline(context, true, inDelivery: false);
      await _loadHistory();
      if (!mounted) return;
      _showSnackBar(l10n.translate('status_available_now_success'));
    } catch (e) {
      if (!mounted) return;
      _showSnackBar('${l10n.translate('generic_try_again_error')}: $e');
    } finally {
      if (mounted) setState(() => _markingAvailable = false);
    }
  }

  void _showSnackBar(String message) {
    if (!mounted) return;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final messenger = ScaffoldMessenger.maybeOf(context);
      if (messenger == null) return;
      messenger.hideCurrentSnackBar();
      messenger.showSnackBar(SnackBar(content: Text(message)));
    });
  }

  // ── Lazy init triggers ───────────────────────────────────────────────────

  void _ensureDataLoaded() {
    final courier = ref.read(currentCourierProvider).asData?.value;
    final isInternal = _isInternalCourier(courier);

    if (isInternal && !_internalScheduleInitialized && !_loadingInternalSchedule) {
      _loadEffectiveWeek();
    }
    if (!_historyInitialized && !_loadingHistory) {
      _loadHistory();
    }
  }

  // ── Build ────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final courier = ref.watch(currentCourierProvider).asData?.value;
    final isInternal = _isInternalCourier(courier);

    if (isInternal && !_internalScheduleInitialized && !_loadingInternalSchedule) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        _loadEffectiveWeek();
      });
    }

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        title: Text(l10n.translate('status_title')),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _StatusHeader(
            courierType: courier?.courierType,
            status: courier?.status,
          ),
          const SizedBox(height: 16),
          if (isInternal)
            _buildInternalSection(l10n)
          else
            _buildRestrictedExternalSection(l10n),
        ],
      ),
    );
  }

  Widget _buildRestrictedExternalSection(AppLocalizations l10n) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: const Color(0xFFFFFBEB),
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: const Color(0xFFFDE68A)),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.lock_outline_rounded, color: Color(0xFFD97706), size: 20),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  l10n.translate('status_internal_readonly_info'),
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF92400E),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  // ── Section builders ─────────────────────────────────────────────────────

  Widget _buildInternalSection(AppLocalizations l10n) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _InfoBanner(message: l10n.translate('status_internal_readonly_info')),
        const SizedBox(height: 12),
        // Problem report card
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: const Color(0xFFE5E7EB)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                l10n.translate('status_internal_report_problem_title'),
                style: const TextStyle(
                  fontWeight: FontWeight.w700,
                  fontSize: 15,
                  color: Color(0xFF111827),
                ),
              ),
              const SizedBox(height: 6),
              Text(
                l10n.translate('status_internal_report_problem_desc'),
                style: TextStyle(
                  color: Colors.grey.shade600,
                  fontSize: 12,
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: _reportingProblem ? null : _openInternalProblemDialog,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: _kRed,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    elevation: 0,
                  ),
                  icon: _reportingProblem
                      ? const SizedBox(
                          height: 14,
                          width: 14,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.report_problem_rounded, size: 18),
                  label: Text(l10n.translate('status_internal_report_problem_button')),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        // Calendar section
        if (_loadingInternalSchedule)
          const Center(child: Padding(
            padding: EdgeInsets.symmetric(vertical: 32),
            child: CircularProgressIndicator(),
          ))
        else if (_internalScheduleInitialized && _effectiveWeek.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 24),
            child: Center(
              child: Text(
                l10n.translate('status_internal_schedule_empty'),
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey.shade500),
              ),
            ),
          )
        else ...[
          _WeekNavigatorBar(
            weekStart: _calendarWeekStart,
            onNavigate: _navigateCalendarWeek,
          ),
          const SizedBox(height: 8),
          _InternalScheduleCard(
            effectiveWeek: _effectiveWeek,
            calendarWeekStart: _calendarWeekStart,
          ),
        ],
        const SizedBox(height: 24),
        _buildHistorySection(l10n),
      ],
    );
  }

  Widget _buildExternalSection(
    AppLocalizations l10n, {
    String? firstName,
    String? status,
  }) {
    final name = (firstName ?? '').trim();
    final displayName = name.isEmpty ? 'Courier' : name;
    final statusValue = (status ?? '').toUpperCase();
    final statusText = statusValue.isEmpty
        ? 'OFFLINE'
        : statusValue.replaceAll('_', ' ');

    final statusColor = switch (statusValue) {
      'AVAILABLE' => const Color(0xFF16A34A),
      'BUSY' || 'ON_DELIVERY' => const Color(0xFFF59E0B),
      'UNAVAILABLE' || 'SUSPENDED' => const Color(0xFFDC2626),
      _ => const Color(0xFF6B7280),
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: const Color(0xFFE5E7EB)),
          ),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Bonjour,',
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey.shade600,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      displayName,
                      style: const TextStyle(
                        fontSize: 26,
                        fontWeight: FontWeight.w800,
                        height: 1.1,
                        color: Color(0xFF111827),
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: statusColor.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.circle, size: 9, color: statusColor),
                    const SizedBox(width: 6),
                    Text(
                      statusText,
                      style: TextStyle(
                        color: statusColor,
                        fontWeight: FontWeight.w700,
                        fontSize: 12,
                        letterSpacing: 0.2,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: const Color(0xFFE5E7EB)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(Icons.edit_calendar_rounded, size: 18, color: _kRed),
                  const SizedBox(width: 8),
                  Text(
                    'Mettre a jour mon etat',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: Colors.grey.shade800,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Text(
                l10n.translate('status_reason_label'),
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey.shade600,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 8),
              DropdownButtonFormField<String>(
                initialValue: _selectedReason,
                decoration: InputDecoration(
                  isDense: true,
                  filled: true,
                  fillColor: const Color(0xFFF3F4F6),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                ),
                items: [
                  DropdownMenuItem(
                      value: 'PANNE',
                      child: Text(l10n.translate('status_reason_panne'))),
                  DropdownMenuItem(
                      value: 'CONGE',
                      child: Text(l10n.translate('status_reason_conge'))),
                  DropdownMenuItem(
                      value: 'ABSENT',
                      child: Text(l10n.translate('status_reason_absent'))),
                  DropdownMenuItem(
                      value: 'RETARD',
                      child: Text(l10n.translate('status_reason_retard'))),
                  DropdownMenuItem(
                      value: 'NE_TRAVAILLE_PAS',
                      child: Text(l10n.translate('status_reason_not_working'))),
                ],
                onChanged: (value) {
                  if (value != null) setState(() => _selectedReason = value);
                },
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _durationController,
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  filled: true,
                  fillColor: const Color(0xFFF3F4F6),
                  labelText: l10n.translate('status_estimated_duration_label'),
                  hintText: 'ex: 30',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _commentController,
                maxLines: 3,
                decoration: InputDecoration(
                  filled: true,
                  fillColor: const Color(0xFFF3F4F6),
                  labelText: l10n.translate('status_comment_label'),
                  hintText: l10n.translate('status_comment_hint'),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: 14),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: _kRed,
                    foregroundColor: Colors.white,
                    elevation: 0,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  onPressed: _submitting ? null : _submitDeclaration,
                  child: _submitting
                      ? const _ButtonLoader()
                      : Text(l10n.translate('status_submit_button')),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFF4B5563),
                    side: const BorderSide(color: Color(0xFFD1D5DB)),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  onPressed: _markingAvailable ? null : _markAvailableNow,
                  child: _markingAvailable
                      ? const _ButtonLoader()
                      : Text(l10n.translate('status_available_button')),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),
        _buildHistorySection(l10n),
      ],
    );
  }

  Widget _buildHistorySection(AppLocalizations l10n) {
    final chips = const [
      ('ALL', 'Tous'),
      ('SUBMITTED', 'Submitted'),
      ('IN_PROGRESS', 'En cours'),
      ('REJECTED', 'Rejetee'),
      ('RESOLVED', 'Resolue'),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          l10n.translate('status_history_title'),
          style: const TextStyle(
            fontSize: 17,
            fontWeight: FontWeight.w700,
            color: Color(0xFF111827),
          ),
        ),
        const SizedBox(height: 10),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: chips.map((entry) {
              final key = entry.$1;
              final label = entry.$2;
              final selected = _historyState == key;
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(label),
                  selected: selected,
                  onSelected: (_) {
                    _updateHistoryState(key);
                  },
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 10),
        if (_loadingHistory)
          const Center(child: CircularProgressIndicator())
        else if (_history.isEmpty)
          Text(l10n.translate('status_history_empty'))
        else
          ..._history.take(10).map((item) => _buildHistoryCard(l10n, item)),
      ],
    );
  }

  DateTime? _parseDateTime(dynamic value) {
    if (value == null) return null;
    final text = value.toString().trim();
    if (text.isEmpty || text == 'null') return null;
    return DateTime.tryParse(text);
  }

  String _formatDateTime(dynamic value) {
    final dt = _parseDateTime(value);
    if (dt == null) return '-';
    final local = dt.toLocal();
    final dd = local.day.toString().padLeft(2, '0');
    final mm = local.month.toString().padLeft(2, '0');
    final yyyy = local.year.toString();
    final hh = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return '$dd/$mm/$yyyy $hh:$min';
  }

  String _historyReasonLabel(AppLocalizations l10n, String code) {
    return switch (code) {
      'PANNE' => l10n.translate('status_reason_panne'),
      'CONGE' => l10n.translate('status_reason_conge'),
      'ABSENT' => l10n.translate('status_reason_absent'),
      'RETARD' => l10n.translate('status_reason_retard'),
      'NE_TRAVAILLE_PAS' => l10n.translate('status_reason_not_working'),
      _ => code,
    };
  }

  String _historyStatusLabel(AppLocalizations l10n, String status) {
    return switch (status) {
      'PENDING_VALIDATION' => l10n.translate('status_state_pending'),
      'APPROVED_ACTIVE' => l10n.translate('status_state_approved'),
      'REJECTED' => l10n.translate('status_state_rejected'),
      'RESOLVED_AVAILABLE' => l10n.translate('status_state_resolved'),
      _ => status,
    };
  }

  Widget _buildHistoryCard(AppLocalizations l10n, Map<String, dynamic> item) {
    final reasonCode = (item['unavailabilityReason'] ?? '-').toString().toUpperCase();
    final validationStatus = (item['validationStatus'] ?? '-').toString().toUpperCase();
    final comment = (item['reason'] ?? '').toString();
    final managerComment = (item['validationComment'] ?? '').toString();

    final submittedAt = item['createdAt'] ?? item['submittedAt'];
    final startsAt = item['startsAt'] ?? item['startDate'];
    final endsAt = item['endsAt'] ?? item['endDate'];
    final decidedAt = item['validatedAt'] ?? item['resolvedAt'];
    final estimatedDuration = item['estimatedDurationMinutes']?.toString();

    final statusColor = switch (validationStatus) {
      'APPROVED_ACTIVE' => const Color(0xFF16A34A),
      'PENDING_VALIDATION' => const Color(0xFFD97706),
      'RESOLVED_AVAILABLE' => const Color(0xFF0F766E),
      'REJECTED' => const Color(0xFFDC2626),
      _ => const Color(0xFF6B7280),
    };

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFE5E7EB)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: statusColor.withValues(alpha: 0.13),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(Icons.event_busy_rounded, size: 18, color: statusColor),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  _historyReasonLabel(l10n, reasonCode),
                  style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 14),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
                decoration: BoxDecoration(
                  color: statusColor.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(
                  _historyStatusLabel(l10n, validationStatus),
                  style: TextStyle(
                    color: statusColor,
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text('${l10n.translate('status_submitted_at')}: ${_formatDateTime(submittedAt)}',
              style: TextStyle(fontSize: 12, color: Colors.grey.shade700)),
          Text('${l10n.translate('status_incident_start')}: ${_formatDateTime(startsAt)}',
              style: TextStyle(fontSize: 12, color: Colors.grey.shade700)),
          Text('${l10n.translate('status_incident_end')}: ${_formatDateTime(endsAt)}',
              style: TextStyle(fontSize: 12, color: Colors.grey.shade700)),
          if (estimatedDuration != null && estimatedDuration.isNotEmpty)
            Text('${l10n.translate('status_estimated_duration_label')}: $estimatedDuration min',
                style: TextStyle(fontSize: 12, color: Colors.grey.shade700)),
          if (decidedAt != null)
            Text('${l10n.translate('status_decided_at')}: ${_formatDateTime(decidedAt)}',
                style: TextStyle(fontSize: 12, color: Colors.grey.shade700)),
          if (comment.isNotEmpty)
            Text('${l10n.translate('status_comment_label')}: $comment',
                style: TextStyle(fontSize: 12, color: Colors.grey.shade700)),
          if (managerComment.isNotEmpty)
            Text('${l10n.translate('status_manager_comment')}: $managerComment',
                style: TextStyle(fontSize: 12, color: Colors.grey.shade700)),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED SMALL WIDGETS
// ─────────────────────────────────────────────────────────────────────────────

class _InfoBanner extends StatelessWidget {
  final String message;
  const _InfoBanner({required this.message});

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        color: colorScheme.primaryContainer.withValues(alpha: 0.35),
      ),
      child: Row(
        children: [
          Icon(Icons.info_outline_rounded, size: 18, color: colorScheme.primary),
          const SizedBox(width: 8),
          Expanded(
            child: Text(message,
                style: Theme.of(context).textTheme.bodyMedium),
          ),
        ],
      ),
    );
  }
}

class _ButtonLoader extends StatelessWidget {
  const _ButtonLoader();

  @override
  Widget build(BuildContext context) => const SizedBox(
        height: 16,
        width: 16,
        child: CircularProgressIndicator(strokeWidth: 2),
      );
}

// ─────────────────────────────────────────────────────────────────────────────
// STATUS HEADER
// ─────────────────────────────────────────────────────────────────────────────

class _StatusHeader extends StatelessWidget {
  final String? courierType;
  final String? status;

  const _StatusHeader({required this.courierType, required this.status});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final typeText =
        courierType?.toUpperCase().trim().isNotEmpty == true ? courierType!.toUpperCase() : '-';
    final statusText = status?.toUpperCase().trim().isNotEmpty == true
        ? status!.toUpperCase().replaceAll('_', ' ')
        : '-';

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        color: theme.colorScheme.surface,
        border: Border.all(
            color: theme.colorScheme.outlineVariant.withValues(alpha: 0.7)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            l10n.translate('status_title'),
            style:
                theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: _HeaderInfoTile(
                  label: l10n.translate('courier'),
                  value: typeText,
                  icon: Icons.badge_outlined,
                  backgroundColor: typeText == 'INTERNAL'
                      ? const Color(0xFFEFF3FF)
                      : const Color(0xFFF4F4F5),
                  textColor: typeText == 'INTERNAL'
                      ? const Color(0xFF2747C7)
                      : const Color(0xFF3F3F46),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _HeaderInfoTile(
                  label: l10n.translate('status_title'),
                  value: statusText,
                  icon: Icons.circle,
                  iconSize: 10,
                  backgroundColor: _statusBgColor(statusText),
                  textColor: _statusFgColor(statusText),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  static Color _statusBgColor(String value) => switch (value) {
        'AVAILABLE' => const Color(0xFFE9F9EF),
        'BUSY' || 'ON DELIVERY' => const Color(0xFFFFF4E5),
        'UNAVAILABLE' || 'SUSPENDED' => const Color(0xFFFDECEC),
        _ => const Color(0xFFF4F4F5),
      };

  static Color _statusFgColor(String value) => switch (value) {
        'AVAILABLE' => const Color(0xFF1C7C43),
        'BUSY' || 'ON DELIVERY' => const Color(0xFFB45309),
        'UNAVAILABLE' || 'SUSPENDED' => const Color(0xFFB42318),
        _ => const Color(0xFF3F3F46),
      };
}

class _HeaderInfoTile extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final double iconSize;
  final Color backgroundColor;
  final Color textColor;

  const _HeaderInfoTile({
    required this.label,
    required this.value,
    required this.icon,
    required this.backgroundColor,
    required this.textColor,
    this.iconSize = 14,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.fromLTRB(10, 9, 10, 10),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.35),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: theme.textTheme.labelSmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 6),
          Container(
            width: double.infinity,
            padding:
                const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
            decoration: BoxDecoration(
              color: backgroundColor,
              borderRadius: BorderRadius.circular(999),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(icon, size: iconSize, color: textColor),
                const SizedBox(width: 6),
                Flexible(
                  child: Text(
                    value,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: textColor,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 0.2,
                    ),
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

// ─────────────────────────────────────────────────────────────────────────────
// WEEK NAVIGATOR BAR
// ─────────────────────────────────────────────────────────────────────────────

class _WeekNavigatorBar extends StatelessWidget {
  final DateTime weekStart;
  final void Function(int delta) onNavigate;

  const _WeekNavigatorBar({required this.weekStart, required this.onNavigate});

  bool get _isCurrentWeek {
    final now = DateTime.now();
    final thisMonday = now.subtract(Duration(days: now.weekday - 1));
    return weekStart.year == thisMonday.year &&
        weekStart.month == thisMonday.month &&
        weekStart.day == thisMonday.day;
  }

  String _formatWeekRange() {
    final end = weekStart.add(const Duration(days: 6));
    final startStr =
        '${weekStart.day} ${_kMonths[weekStart.month]}';
    final endStr =
        '${end.day} ${_kMonths[end.month]} ${end.year}';
    if (weekStart.month == end.month) {
      return '${weekStart.day} - ${end.day} ${_kMonths[end.month]} ${end.year}';
    }
    return '$startStr - $endStr';
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        IconButton(
          onPressed: () => onNavigate(-1),
          icon: const Icon(Icons.chevron_left_rounded),
          style: IconButton.styleFrom(
            backgroundColor: const Color(0xFFF3F4F6),
            shape: const CircleBorder(),
            padding: const EdgeInsets.all(6),
          ),
          iconSize: 20,
        ),
        Expanded(
          child: GestureDetector(
            onTap: _isCurrentWeek ? null : () => onNavigate(0), // handled by parent
            child: Column(
              children: [
                Text(
                  _isCurrentWeek ? 'Cette semaine' : _formatWeekRange(),
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: _isCurrentWeek ? _kRed : const Color(0xFF374151),
                  ),
                ),
                if (!_isCurrentWeek)
                  Text(
                    _formatWeekRange(),
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 11,
                      color: Colors.grey.shade500,
                    ),
                  ),
              ],
            ),
          ),
        ),
        IconButton(
          onPressed: () => onNavigate(1),
          icon: const Icon(Icons.chevron_right_rounded),
          style: IconButton.styleFrom(
            backgroundColor: const Color(0xFFF3F4F6),
            shape: const CircleBorder(),
            padding: const EdgeInsets.all(6),
          ),
          iconSize: 20,
        ),
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// INTERNAL SCHEDULE CARD
// ─────────────────────────────────────────────────────────────────────────────

class _InternalScheduleCard extends StatelessWidget {
  /// List of 7 EffectiveScheduleResponse maps (Mon → Sun) from the backend.
  /// Each entry has: status, date, dayOfWeek, isRestDay, shifts[], exception?
  final List<Map<String, dynamic>> effectiveWeek;
  final DateTime calendarWeekStart;

  const _InternalScheduleCard({
    required this.effectiveWeek,
    required this.calendarWeekStart,
  });

  /// Shifts for a given day index (0=Mon…6=Sun), from the effective week data.
  List<Map<String, dynamic>> _shiftsFor(int index) {
    if (index >= effectiveWeek.length) return const [];
    
    // On cherche le jour qui correspond à la date attendue (calendarWeekStart + index)
    final targetDate = calendarWeekStart.add(Duration(days: index));
    final targetDateStr = '${targetDate.year}-${targetDate.month.toString().padLeft(2, '0')}-${targetDate.day.toString().padLeft(2, '0')}';
    
    try {
      final dayData = effectiveWeek.firstWhere(
        (day) => day['date'].toString().startsWith(targetDateStr),
        orElse: () => <String, dynamic>{},
      );
      
      final raw = dayData['shifts'];
      if (raw is! List) return const [];
      return raw.whereType<Map<String, dynamic>>().toList();
    } catch (_) {
      return const [];
    }
  }

  /// Exception map for a day index, or null if no exception.
  Map<String, dynamic>? _exceptionFor(int index) {
    if (index >= effectiveWeek.length) return null;
    
    final targetDate = calendarWeekStart.add(Duration(days: index));
    final targetDateStr = '${targetDate.year}-${targetDate.month.toString().padLeft(2, '0')}-${targetDate.day.toString().padLeft(2, '0')}';
    
    try {
      final dayData = effectiveWeek.firstWhere(
        (day) => day['date'].toString().startsWith(targetDateStr),
        orElse: () => <String, dynamic>{},
      );
      
      final ex = dayData['exception'];
      if (ex is Map<String, dynamic>) {
        final normalized = Map<String, dynamic>.from(ex);
        normalized['startsAt'] = _readFirstNonEmpty(
          normalized,
          ['startsAt', 'startAt', 'starts_at', 'startTime', 'start_time'],
        );
        normalized['endsAt'] = _readFirstNonEmpty(
          normalized,
          ['endsAt', 'endAt', 'ends_at', 'endTime', 'end_time'],
        );
        return normalized;
      }
    } catch (_) {}
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    // Build dayShifts map for _CalendarView
    final dayShifts = {
      for (var i = 0; i < _kDays.length; i++)
        _kDays[i]: _shiftsFor(i),
    };

    // Build exceptions map by day-index for _CalendarView
    final exceptionsMap = {
      for (var i = 0; i < _kDays.length; i++)
        i: _exceptionFor(i),
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Google-Calendar-style week view
        _CalendarView(
          dayShifts: dayShifts,
          weekStart: calendarWeekStart,
          exceptionsMap: exceptionsMap,
          l10n: l10n,
        ),
        const SizedBox(height: 16),
        // Day-by-day detail list
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              l10n.translate('status_internal_week_details_title'),
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: Color(0xFF1A1A1A),
              ),
            ),
            Icon(Icons.calendar_month_rounded, size: 20, color: Colors.grey.shade500),
          ],
        ),
        const SizedBox(height: 12),
        ...List.generate(_kDays.length, (index) {
          final day = _kDays[index];
          final date = calendarWeekStart.add(Duration(days: index));
          final dateLabel = '${date.day} ${_kMonths[date.month]}';
          final exception = _exceptionFor(index);
          final shifts = _shiftsFor(index);
          
          return _DayRow(
            dayLabel: l10n.translate('status_day_${day.toLowerCase()}'),
            dateLabel: dateLabel,
            shifts: shifts,
            exception: exception,
            l10n: l10n,
          );
        }),
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// GOOGLE-CALENDAR-STYLE WEEK VIEW
// ─────────────────────────────────────────────────────────────────────────────

class _CalendarView extends StatefulWidget {
  final Map<String, List<Map<String, dynamic>>> dayShifts;
  final DateTime weekStart;
  /// Maps day-index (0=Mon…6=Sun) → exception map (or null).
  final Map<int, Map<String, dynamic>?> exceptionsMap;
  final AppLocalizations l10n;

  const _CalendarView({
    required this.dayShifts,
    required this.weekStart,
    required this.exceptionsMap,
    required this.l10n,
  });

  @override
  State<_CalendarView> createState() => _CalendarViewState();
}

class _CalendarViewState extends State<_CalendarView> {
  late final ScrollController _verticalScroll;

  static const int _startHour = 6;
  static const int _endHour = 23;
  static const double _hourHeight = 52.0;
  static const double _timeAxisWidth = 46.0;
  static const double _calendarBodyHeight = 300.0;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    final targetHour = now.hour.clamp(_startHour, _endHour - 1) - _startHour;
    // Scroll so current hour is near the top (with 1h offset for context)
    final initialOffset = ((targetHour - 1).clamp(0, _endHour - _startHour - 1)) * _hourHeight;
    _verticalScroll = ScrollController(initialScrollOffset: initialOffset);
  }

  @override
  void dispose() {
    _verticalScroll.dispose();
    super.dispose();
  }

  bool _isToday(int dayIndex) {
    final date = widget.weekStart.add(Duration(days: dayIndex));
    final now = DateTime.now();
    return date.year == now.year &&
        date.month == now.month &&
        date.day == now.day;
  }

  Map<String, dynamic>? _exceptionForDay(int dayIndex) {
    return widget.exceptionsMap[dayIndex];
  }

  @override
  Widget build(BuildContext context) {
    final totalCalendarHeight = (_endHour - _startHour) * _hourHeight;

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFE5E7EB)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(14),
        child: LayoutBuilder(
          builder: (context, constraints) {
            final availableWidth = constraints.maxWidth - _timeAxisWidth;
            final dayWidth = max(38.0, availableWidth / 7);
            return Column(
              children: [
                // ── Day header row ──────────────────────────────────────
                _buildDayHeader(dayWidth),
                Divider(height: 1, color: Colors.grey.shade200),
                // ── Scrollable body ─────────────────────────────────────
                SizedBox(
                  height: _calendarBodyHeight,
                  child: SingleChildScrollView(
                    controller: _verticalScroll,
                    child: SizedBox(
                      height: totalCalendarHeight,
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Time axis
                          _buildTimeAxis(totalCalendarHeight),
                          // Day columns
                          ...List.generate(7, (i) => _buildDayColumn(i, dayWidth, totalCalendarHeight)),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _buildDayHeader(double dayWidth) {
    final now = DateTime.now();
    return SizedBox(
      height: 56,
      child: Row(
        children: [
          const SizedBox(width: _timeAxisWidth),
          ...List.generate(7, (i) {
            final date = widget.weekStart.add(Duration(days: i));
            final isToday = date.year == now.year &&
                date.month == now.month &&
                date.day == now.day;
            final dayKey = _kDays[i].toLowerCase();
            final dayName = widget.l10n.translate('status_day_$dayKey');
            final shortName = dayName.length > 3
                ? dayName.substring(0, 3).toUpperCase()
                : dayName.toUpperCase();
            final hasException = _exceptionForDay(i) != null;

            return Container(
              width: dayWidth,
              decoration: BoxDecoration(
                color: isToday
                    ? const Color(0xFFFFF5F5)
                    : Colors.transparent,
                border: Border(
                  left: BorderSide(color: Colors.grey.shade200),
                ),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    shortName,
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: isToday
                          ? _kRed
                          : const Color(0xFF9CA3AF),
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Stack(
                    alignment: Alignment.topRight,
                    children: [
                      Container(
                        width: 30,
                        height: 30,
                        decoration: BoxDecoration(
                          color: isToday ? _kRed : Colors.transparent,
                          shape: BoxShape.circle,
                        ),
                        child: Center(
                          child: Text(
                            '${date.day}',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w800,
                              color: isToday
                                  ? Colors.white
                                  : const Color(0xFF374151),
                            ),
                          ),
                        ),
                      ),
                      if (hasException)
                        Positioned(
                          top: 0,
                          right: 0,
                          child: Container(
                            width: 8,
                            height: 8,
                            decoration: const BoxDecoration(
                              color: Color(0xFFDC2626),
                              shape: BoxShape.circle,
                            ),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildTimeAxis(double totalHeight) {
    return SizedBox(
      width: _timeAxisWidth,
      height: totalHeight,
      child: Stack(
        children: List.generate(_endHour - _startHour, (i) {
          final hour = _startHour + i;
          return Positioned(
            top: i * _hourHeight - 7,
            left: 0,
            right: 4,
            child: Text(
              '${hour.toString().padLeft(2, '0')}:00',
              textAlign: TextAlign.right,
              style: TextStyle(
                fontSize: 10,
                color: Colors.grey.shade400,
                fontWeight: FontWeight.w500,
              ),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildDayColumn(int dayIndex, double dayWidth, double totalHeight) {
    final day = _kDays[dayIndex];
    final shifts = widget.dayShifts[day] ?? const [];
    final isToday = _isToday(dayIndex);
    final exception = _exceptionForDay(dayIndex);
    // Full-day rest: grey background only when no specific hours are set
    final hasTimeRange = _hasExceptionTimeRange(exception);
    final isFullDayRest = exception != null && exception['isRestPeriod'] == true && !hasTimeRange;
    final now = DateTime.now();

    return Container(
      width: dayWidth,
      height: totalHeight,
      decoration: BoxDecoration(
        color: isFullDayRest
            ? const Color(0xFFF1F5F9) // Grey background for full-day rest exceptions
            : (isToday ? const Color(0xFFFFF8F8) : Colors.white),
        border: Border(
          left: BorderSide(color: Colors.grey.shade100),
        ),
      ),
      child: Stack(
        children: [
          // Hour grid lines
          ...List.generate(_endHour - _startHour, (i) {
            return Positioned(
              top: i * _hourHeight,
              left: 0,
              right: 0,
              child: Container(
                height: _hourHeight,
                decoration: BoxDecoration(
                  border: Border(
                    top: BorderSide(
                      color: i == 0
                          ? Colors.transparent
                          : Colors.grey.shade100,
                    ),
                  ),
                ),
              ),
            );
          }),

          // Shift blocks — hidden on full-day rest exception days
          if (!isFullDayRest)
            ...shifts.expand<Widget>((shift) {
            final startRaw =
                (shift['startTime'] ?? shift['start'] ?? '').toString();
            final endRaw =
                (shift['endTime'] ?? shift['end'] ?? '').toString();
            final start = _parseHourValue(startRaw);
            final end = _parseHourValue(endRaw);
            if (start == null || end == null || end <= start) return const [];
            if (end <= _startHour || start >= _endHour) return const [];

            final clampedStart =
                start.clamp(_startHour.toDouble(), _endHour.toDouble());
            final clampedEnd =
                end.clamp(_startHour.toDouble(), _endHour.toDouble());

            final breakStartRaw =
                (shift['breakStart'] ?? '').toString().trim();
            final breakEndRaw = (shift['breakEnd'] ?? '').toString().trim();
            final bStart = _parseHourValue(breakStartRaw);
            final bEnd = _parseHourValue(breakEndRaw);
            final hasBreak = bStart != null &&
                bEnd != null &&
                bEnd > bStart &&
                bStart >= clampedStart &&
                bEnd <= clampedEnd;

            Widget shiftBlock({
              required double fromH,
              required double toH,
              required bool isFirst,
              required bool isLast,
              required String label,
            }) {
              final top = (fromH - _startHour) * _hourHeight;
              final height =
                  ((toH - fromH) * _hourHeight).clamp(4.0, totalHeight - top);
              return Positioned(
                top: top,
                left: 3,
                right: 3,
                height: height.toDouble(),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 5, vertical: 3),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [_kRed, _kRedDark],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.only(
                      topLeft: isFirst
                          ? const Radius.circular(6)
                          : Radius.zero,
                      topRight: isFirst
                          ? const Radius.circular(6)
                          : Radius.zero,
                      bottomLeft: isLast
                          ? const Radius.circular(6)
                          : Radius.zero,
                      bottomRight: isLast
                          ? const Radius.circular(6)
                          : Radius.zero,
                    ),
                    boxShadow: isFirst
                        ? [
                            BoxShadow(
                              color: _kRed.withValues(alpha: 0.3),
                              blurRadius: 4,
                              offset: const Offset(0, 2),
                            ),
                          ]
                        : null,
                  ),
                  child: FittedBox(
                    alignment: Alignment.topLeft,
                    fit: BoxFit.scaleDown,
                    child: Text(
                      label,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
              );
            }

            if (!hasBreak) {
              final top = (clampedStart - _startHour) * _hourHeight;
              final height = ((clampedEnd - clampedStart) * _hourHeight)
                  .clamp(16.0, totalHeight - top);
              return [
                Positioned(
                  top: top,
                  left: 3,
                  right: 3,
                  height: height.toDouble(),
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 5, vertical: 4),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [_kRed, _kRedDark],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      borderRadius: BorderRadius.circular(6),
                      boxShadow: [
                        BoxShadow(
                          color: _kRed.withValues(alpha: 0.3),
                          blurRadius: 4,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: FittedBox(
                      alignment: Alignment.topLeft,
                      fit: BoxFit.scaleDown,
                      child: Text(
                        '${_normalizeTime(startRaw)}-${_normalizeTime(endRaw)}',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ),
                ),
              ];
            }

            // ── Split into 3 visual blocks: work / pause / work ──────
            final breakTopH = bStart;
            final breakBotH = bEnd;
            final breakTop = (breakTopH - _startHour) * _hourHeight;
            final breakHeight =
                ((breakBotH - breakTopH) * _hourHeight).clamp(4.0, 999.0);

            return [
              // Pre-break work block
              shiftBlock(
                fromH: clampedStart,
                toH: breakTopH,
                isFirst: true,
                isLast: false,
                label:
                    '${_normalizeTime(startRaw)}-${_normalizeTime(breakStartRaw)}',
              ),
              // Pause gap block
              Positioned(
                top: breakTop,
                left: 3,
                right: 3,
                height: breakHeight.toDouble(),
                child: Container(
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF3E0),
                    border: Border.all(
                      color: const Color(0xFFFF9800),
                      width: 1,
                    ),
                  ),
                  padding: const EdgeInsets.symmetric(
                      horizontal: 5, vertical: 3),
                  child: FittedBox(
                    alignment: Alignment.topLeft,
                    fit: BoxFit.scaleDown,
                    child: Text(
                      '${widget.l10n.translate('status_internal_break_label')} ${_normalizeTime(breakStartRaw)}-${_normalizeTime(breakEndRaw)}',
                      style: const TextStyle(
                        fontSize: 9,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFFE65100),
                      ),
                    ),
                  ),
                ),
              ),
              // Post-break work block
              shiftBlock(
                fromH: breakBotH,
                toH: clampedEnd,
                isFirst: false,
                isLast: true,
                label:
                    '${_normalizeTime(breakEndRaw)}-${_normalizeTime(endRaw)}',
              ),
            ];
          }),

          // Exception overlay — rendered ON TOP of shift blocks
          // Si startsAt/endsAt sont définis → overlay partiel (heures précises)
          // Sinon → overlay toute la journée
          if (exception != null)
            _buildExceptionOverlay(exception, totalHeight),

          // Current time indicator (today only)
          if (isToday) ...[
            Builder(builder: (_) {
              final currentHour = now.hour + now.minute / 60.0;
              if (currentHour < _startHour || currentHour > _endHour) {
                return const SizedBox.shrink();
              }
              final top = (currentHour - _startHour) * _hourHeight;
              return Positioned(
                top: top.clamp(0.0, totalHeight - 2),
                left: 0,
                right: 0,
                child: Row(
                  children: [
                    Container(
                      width: 9,
                      height: 9,
                      decoration: const BoxDecoration(
                        color: _kRed,
                        shape: BoxShape.circle,
                      ),
                    ),
                    Expanded(
                      child: Container(height: 2, color: _kRed),
                    ),
                  ],
                ),
              );
            }),
          ],
        ],
      ),
    );
  }

  /// Build exception overlay — partial (hour-based) or full-day.
  Widget _buildExceptionOverlay(Map<String, dynamic> exception, double totalHeight) {
    final exStart = _parseExceptionHour(
      exception,
      ['startsAt', 'startAt', 'starts_at', 'startTime', 'start_time'],
    );
    final exEnd = _parseExceptionHour(
      exception,
      ['endsAt', 'endAt', 'ends_at', 'endTime', 'end_time'],
    );

    if (exStart != null && exEnd != null && exEnd > exStart) {
      // Partial overlay — only cover the specified hours
      final clampedStart = exStart.clamp(_startHour.toDouble(), _endHour.toDouble());
      final clampedEnd = exEnd.clamp(_startHour.toDouble(), _endHour.toDouble());
      final top = (clampedStart - _startHour) * _hourHeight;
      final height = ((clampedEnd - clampedStart) * _hourHeight).clamp(4.0, totalHeight - top);

      return Positioned(
        top: top,
        left: 0,
        right: 0,
        height: height.toDouble(),
        child: _ExceptionOverlay(exception: exception),
      );
    }

    // Full-day overlay
    return Positioned.fill(
      child: _ExceptionOverlay(exception: exception),
    );
  }
}

/// Diagonal-stripe overlay shown on days with an active exception.
class _ExceptionOverlay extends StatelessWidget {
  final Map<String, dynamic> exception;

  const _ExceptionOverlay({required this.exception});

  String get _label {
    final type = (exception['exceptionType'] ?? '').toString();
    final lbl = (exception['label'] ?? '').toString().trim();
    if (lbl.isNotEmpty) return lbl;
    return switch (type) {
      'JOUR_FERIE' => 'Jour férié',
      'CONGE' => 'Congé',
      'FERMETURE' => 'Fermeture',
      'FORMATION' => 'Formation',
      'EVENEMENT_SPECIAL' => 'Événement',
      'PANNE' => 'Panne',
      'ABSENT' => 'Absent',
      'RETARD' => 'Retard',
      'NE_TRAVAILLE_PAS' => 'Ne travaille pas',
      _ => 'Exception',
    };
  }

  @override
  Widget build(BuildContext context) {
    final isRest = exception['isRestPeriod'] == true;
    final color = isRest ? const Color(0xFF64748B) : const Color(0xFFDC2626);
    
    return Container(
      color: color.withValues(alpha: isRest ? 0.15 : 0.07),
      child: Center(
        child: RotatedBox(
          quarterTurns: 3,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.8),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              _label.toUpperCase(),
              style: const TextStyle(
                fontSize: 9,
                fontWeight: FontWeight.w900,
                color: Colors.white,
                letterSpacing: 1.0,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DAY ROW (detail list below the calendar)
// ─────────────────────────────────────────────────────────────────────────────

class _DayRow extends StatelessWidget {
  final String dayLabel;
  final String dateLabel;
  final List<Map<String, dynamic>> shifts;
  final Map<String, dynamic>? exception;
  final AppLocalizations l10n;

  const _DayRow({
    required this.dayLabel,
    required this.dateLabel,
    required this.shifts,
    required this.l10n,
    this.exception,
  });

  @override
  Widget build(BuildContext context) {
    // Check if exception has specific hours (partial day) vs full day
    final hasTimeRange = _hasExceptionTimeRange(exception);
    final isFullDayRest = exception != null && exception!['isRestPeriod'] == true && !hasTimeRange;

    // 1. Exception journée complète (Congé, jour férié, etc.) → tuile exception seule
    if (isFullDayRest) {
      return _ExceptionDayTile(
        dayLabel: dayLabel,
        dateLabel: dateLabel,
        exception: exception!,
        l10n: l10n,
      );
    }

    // 2. Pas de shifts (et pas d'exception) → Jour de repos standard
    if (shifts.isEmpty && exception == null) {
      return _OffDayTile(
        dayLabel: dayLabel,
        dateLabel: dateLabel,
        l10n: l10n,
      );
    }

    // 3. Shifts normaux (avec ou sans exception horaire)
    return Column(
      children: [
        if (exception != null)
          _SpecialEventTag(exception: exception!),
        ...shifts.map((shift) => _WorkShiftTile(
              dayLabel: dayLabel,
              dateLabel: dateLabel,
              shift: shift,
              l10n: l10n,
            )),
      ],
    );
  }
}

/// Petit tag discret affiché au-dessus des shifts quand il y a une exception non-repos.
class _SpecialEventTag extends StatelessWidget {
  final Map<String, dynamic> exception;
  const _SpecialEventTag({required this.exception});

  @override
  Widget build(BuildContext context) {
    final lbl = (exception['label'] ?? '').toString().trim();
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 6),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFEFF6FF),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFBFDBFE)),
      ),
      child: Row(
        children: [
          const Icon(Icons.info_outline_rounded, size: 14, color: Color(0xFF2563EB)),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              lbl.isNotEmpty ? lbl : 'Événement spécial',
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: Color(0xFF1E40AF),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ExceptionDayTile extends StatelessWidget {
  final String dayLabel;
  final String dateLabel;
  final Map<String, dynamic> exception;
  final AppLocalizations l10n;

  const _ExceptionDayTile({
    required this.dayLabel,
    required this.dateLabel,
    required this.exception,
    required this.l10n,
  });

  String get _exceptionLabel {
    final lbl = (exception['label'] ?? '').toString().trim();
    if (lbl.isNotEmpty) return lbl;
    final type = (exception['exceptionType'] ?? '').toString();
    return switch (type) {
      'JOUR_FERIE' => 'Jour férié',
      'CONGE' => 'Congé',
      'FERMETURE' => 'Fermeture',
      'FORMATION' => 'Formation',
      'EVENEMENT_SPECIAL' => 'Événement spécial',
      _ => 'Exception',
    };
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFFEF2F2),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFFCA5A5)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          _DayLabel(dayLabel: dayLabel, dateLabel: dateLabel, isWorking: false),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.event_busy_rounded,
                  size: 14, color: Color(0xFFDC2626)),
              const SizedBox(width: 6),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(
                  color: const Color(0xFFDC2626).withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  _exceptionLabel,
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: Color(0xFFDC2626),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _OffDayTile extends StatelessWidget {
  final String dayLabel;
  final String dateLabel;
  final AppLocalizations l10n;

  const _OffDayTile({
    required this.dayLabel,
    required this.dateLabel,
    required this.l10n,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          _DayLabel(dayLabel: dayLabel, dateLabel: dateLabel, isWorking: false),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 5),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: Colors.grey.shade300),
            ),
            child: Text(
              l10n.translate('status_internal_day_off'),
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: Colors.grey.shade500,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _WorkShiftTile extends StatelessWidget {
  final String dayLabel;
  final String dateLabel;
  final Map<String, dynamic> shift;
  final AppLocalizations l10n;

  const _WorkShiftTile({
    required this.dayLabel,
    required this.dateLabel,
    required this.shift,
    required this.l10n,
  });

  @override
  Widget build(BuildContext context) {
    final start =
        _normalizeTime((shift['startTime'] ?? shift['start'] ?? '-').toString());
    final end =
        _normalizeTime((shift['endTime'] ?? shift['end'] ?? '-').toString());
    final breakStart = (shift['breakStart'] ?? '').toString().trim();
    final breakEnd = (shift['breakEnd'] ?? '').toString().trim();
    final hasBreak = breakStart.isNotEmpty && breakEnd.isNotEmpty;

    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [_kRed, _kRedDark],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Expanded(
            child: _DayLabel(
                dayLabel: dayLabel,
                dateLabel: dateLabel,
                isWorking: true),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Directionality(
                textDirection: TextDirection.ltr,
                child: Text(
                  '$start - $end',
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                    color: Colors.white,
                    letterSpacing: 0.3,
                  ),
                ),
              ),
              if (hasBreak) ...[
                const SizedBox(height: 4),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.coffee_rounded,
                        size: 12, color: Colors.white70),
                    const SizedBox(width: 4),
                    Directionality(
                      textDirection: TextDirection.ltr,
                      child: Text(
                        '${l10n.translate('status_internal_break_label')}: '
                        '${_normalizeTime(breakStart)} - ${_normalizeTime(breakEnd)}',
                        style: const TextStyle(
                            fontSize: 11, color: Colors.white70),
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
          const SizedBox(width: 10),
          const Icon(Icons.chevron_right_rounded,
              color: Colors.white70, size: 22),
        ],
      ),
    );
  }
}

class _DayLabel extends StatelessWidget {
  final String dayLabel;
  final String dateLabel;
  final bool isWorking;

  const _DayLabel({
    required this.dayLabel,
    required this.dateLabel,
    required this.isWorking,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          dayLabel,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w700,
            color: isWorking ? Colors.white : Colors.grey.shade700,
          ),
        ),
        if (dateLabel.isNotEmpty) ...[
          const SizedBox(height: 2),
          Text(
            dateLabel,
            style: TextStyle(
              fontSize: 11,
              color: isWorking ? Colors.white60 : Colors.grey.shade400,
            ),
          ),
        ],
      ],
    );
  }
}
