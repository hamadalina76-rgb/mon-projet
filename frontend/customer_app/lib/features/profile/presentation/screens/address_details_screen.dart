import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import '../../../../config/routes/route_names.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../location/data/models/saved_location.dart';
import '../../../location/presentation/providers/location_provider.dart';
import '../../data/models/address_model.dart';
import '../../data/datasources/address_api_service.dart';
import '../providers/address_provider.dart';

/// Écran de saisie des détails d'une adresse.
/// Affiché après la sélection du type dans [AddressTypeSelectorScreen].
class AddressDetailsScreen extends ConsumerStatefulWidget {
  final AddressType type;
  final String customerId;
  final SavedLocation? fromLocation;

  /// Si non-null, on est en mode édition
  final AddressModel? existing;

  /// Si fourni, navigue vers cette route après la sauvegarde réussie
  /// plutôt que de faire un pop().
  final String? redirectOnSuccess;

  const AddressDetailsScreen({
    super.key,
    required this.type,
    required this.customerId,
    this.fromLocation,
    this.existing,
    this.redirectOnSuccess,
  });

  @override
  ConsumerState<AddressDetailsScreen> createState() =>
      _AddressDetailsScreenState();
}

class _AddressDetailsScreenState extends ConsumerState<AddressDetailsScreen> {
  final _formKey = GlobalKey<FormState>();

  // Controllers
  late final TextEditingController _addressCtrl;
  late final TextEditingController _buildingCtrl;
  late final TextEditingController _floorCtrl;
  late final TextEditingController _doorCtrl;
  late final TextEditingController _infoCtrl;
  late final TextEditingController _customLabelCtrl;

  // Label selection
  String? _selectedPresetLabel;
  bool _useCustomLabel = false;
  bool _isLoading = false;
  String? _errorMessage;

  // Address type (mutable — can be changed in edit mode)
  late AddressType _currentType;

  // UX helpers
  bool _labelHasError = false;
  bool _isDeleting = false;
  bool _labelsInitialized = false;
  final ScrollController _scrollCtrl = ScrollController();

  List<String> get _presetLabels {
    final l10n = AppLocalizations.of(context)!;
    switch (_currentType) {
      case AddressType.home:
        return [l10n.translate('address_type_home')];
      case AddressType.work:
        return [l10n.translate('address_type_work')];
      case AddressType.apartment:
        return [l10n.translate('address_type_apartment')];
      case AddressType.other:
        return [];
    }
  }

  String get _screenTitle {
    final l10n = AppLocalizations.of(context)!;
    switch (_currentType) {
      case AddressType.home:      return l10n.translate('home_screen_title');
      case AddressType.work:      return l10n.translate('work_screen_title');
      case AddressType.apartment: return l10n.translate('apartment_screen_title');
      case AddressType.other:     return l10n.translate('other_screen_title');
    }
  }

  Color get _typeColor {
    switch (_currentType) {
      case AddressType.home:      return AppColors.primary;
      case AddressType.work:      return AppColors.primaryDark;
      case AddressType.apartment: return AppColors.secondary;
      case AddressType.other:     return AppColors.secondaryDark;
    }
  }

  IconData get _typeIcon {
    switch (_currentType) {
      case AddressType.home:      return Icons.home_rounded;
      case AddressType.work:      return Icons.business_rounded;
      case AddressType.apartment: return Icons.apartment_rounded;
      case AddressType.other:     return Icons.location_on_rounded;
    }
  }

  @override
  void initState() {
    super.initState();
    final e = widget.existing;
    final loc = widget.fromLocation;
    _currentType = e?.type ?? widget.type;

    _addressCtrl  = TextEditingController(
        text: e?.formattedAddress ?? loc?.formattedAddress ?? '');
    _buildingCtrl = TextEditingController(text: e?.building ?? '');
    _floorCtrl    = TextEditingController(text: e?.floor ?? '');
    _doorCtrl     = TextEditingController(text: e?.apartment ?? '');
    _infoCtrl     = TextEditingController(text: e?.deliveryInstructions ?? '');
    _customLabelCtrl = TextEditingController();
    // Clear label error when the user types a new custom label
    _customLabelCtrl.addListener(() {
      if (_labelHasError && _customLabelCtrl.text.isNotEmpty) {
        setState(() {
          _labelHasError = false;
          _errorMessage = null;
        });
      }
    });

    // Pre-select preset label if editing
    // NOTE: _presetLabels needs l10n → deferred to didChangeDependencies
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_labelsInitialized) return;
    _labelsInitialized = true;
    final e = widget.existing;
    // Pre-select preset label if editing
    if (e?.label != null) {
      if (_presetLabels.contains(e!.label)) {
        _selectedPresetLabel = e.label;
      } else {
        _useCustomLabel = true;
        _customLabelCtrl.text = e.label!;
      }
    } else if (_presetLabels.isNotEmpty) {
      _selectedPresetLabel = _presetLabels.first;
    } else {
      // OTHER type: always custom
      _useCustomLabel = true;
    }
  }

  @override
  void dispose() {
    _addressCtrl.dispose();
    _buildingCtrl.dispose();
    _floorCtrl.dispose();
    _doorCtrl.dispose();
    _infoCtrl.dispose();
    _customLabelCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  void _onTypeChanged(AddressType newType) {
    setState(() {
      _currentType = newType;
      if (newType == AddressType.other) {
        // Switching TO Other → require custom label
        _selectedPresetLabel = null;
        _useCustomLabel = true;
        _customLabelCtrl.clear();
      } else {
        // Switching FROM Other (or between non-Other types) → reset to first preset
        if (_useCustomLabel) _customLabelCtrl.clear();
        _useCustomLabel = false;
        final presets = _presetLabels;
        _selectedPresetLabel = presets.isNotEmpty ? presets.first : null;
      }
      _labelHasError = false;
      _errorMessage = null;
    });
  }

  // ── Delete ────────────────────────────────────────────────────────────────

  Future<void> _delete() async {
    final address = widget.existing;
    if (address == null) return;
    final l10n = AppLocalizations.of(context)!;

    // Confirmation dialog
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text(l10n.translate('delete_address'),
            style: const TextStyle(fontWeight: FontWeight.w700)),
        content: Text(
            '"${address.displayLabel}" — ${l10n.translate('delete_address_confirm')}'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.translate('cancel')),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.primary),
            child: Text(l10n.translate('delete')),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    setState(() => _isDeleting = true);
    try {
      // Delete from database
      await ref.read(addressNotifierProvider.notifier).deleteAddress(address.id);
      if (!mounted) return;

      final l10n2 = AppLocalizations.of(context)!;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.delete_outline_rounded, color: AppColors.surface, size: 20),
              const SizedBox(width: 10),
              Expanded(
                child: Text(l10n2.translate('address_deleted'),
                    style: const TextStyle(
                        color: AppColors.surface, fontWeight: FontWeight.w600)),
              ),
            ],
          ),
          backgroundColor: AppColors.primaryDark,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          duration: const Duration(seconds: 3),
        ),
      );

      if (address.isDefault) {
        // Default address deleted → detect current GPS position and open confirm location
        double lat = 36.8065; // fallback: Tunis
        double lng = 10.1815;
        String initialAddress = '';

        try {
          LocationPermission permission = await Geolocator.checkPermission();
          if (permission == LocationPermission.denied) {
            permission = await Geolocator.requestPermission();
          }
          final hasPermission = permission == LocationPermission.whileInUse ||
              permission == LocationPermission.always;
          if (hasPermission && await Geolocator.isLocationServiceEnabled()) {
            final position = await Geolocator.getCurrentPosition(
              desiredAccuracy: LocationAccuracy.high,
              timeLimit: const Duration(seconds: 12),
            );
            lat = position.latitude;
            lng = position.longitude;
          }
        } catch (_) {}

        if (!mounted) return;
        ref.read(locationNotifierProvider.notifier).setLoading();
        context.go(RouteNames.confirmLocation, extra: {
          'latitude': lat,
          'longitude': lng,
          'initialAddress': initialAddress,
        });
      } else {
        context.pop(true);
      }
    } catch (e) {
      if (mounted) {
        final l10n2 = AppLocalizations.of(context)!;
        setState(() {
          _errorMessage = l10n2.translate('address_delete_error');
        });
      }
    } finally {
      if (mounted) setState(() => _isDeleting = false);
    }
  }

  String? get _resolvedLabel {
    if (_useCustomLabel) {
      return _customLabelCtrl.text.trim().isEmpty
          ? null
          : _customLabelCtrl.text.trim();
    }
    return _selectedPresetLabel;
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final l10n = AppLocalizations.of(context)!;

    // Validate OTHER custom label
    if (_currentType == AddressType.other && _resolvedLabel == null) {
      setState(() => _errorMessage = l10n.translate('label_required'));
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    // Prefer explicit street/city from the GPS location; fall back to existing.
    final streetVal = widget.fromLocation?.street.isNotEmpty == true
        ? widget.fromLocation!.street
        : widget.existing?.street;
    final cityVal = widget.fromLocation?.city.isNotEmpty == true
        ? widget.fromLocation!.city
        : widget.existing?.city;

    final request = AddressRequest(
      type: _currentType,
      label: _resolvedLabel,
      street: streetVal,
      city: cityVal,
      formattedAddress: _addressCtrl.text.trim().isEmpty
          ? null
          : _addressCtrl.text.trim(),
      building: _currentType == AddressType.apartment
          ? (_buildingCtrl.text.trim().isEmpty ? null : _buildingCtrl.text.trim())
          : null,
      floor: _floorCtrl.text.trim().isEmpty ? null : _floorCtrl.text.trim(),
      apartment: _doorCtrl.text.trim().isEmpty ? null : _doorCtrl.text.trim(),
      deliveryInstructions: _infoCtrl.text.trim().isEmpty
          ? null
          : _infoCtrl.text.trim(),
      latitude: widget.fromLocation?.latitude ?? widget.existing?.latitude,
      longitude: widget.fromLocation?.longitude ?? widget.existing?.longitude,
    );

    try {
      final notifier = ref.read(addressNotifierProvider.notifier);
      if (widget.existing != null) {
        await notifier.updateAddress(widget.existing!.id, request);
      } else {
        await notifier.createAddress(widget.customerId, request);
        // Reflect the saved address label on the explore screen header
        if (widget.fromLocation != null) {
          final label = _resolvedLabel;
          if (label != null && label.isNotEmpty) {
            await ref
                .read(locationNotifierProvider.notifier)
                .tagLocation(widget.type, label);
          }
        }
      }
      if (!mounted) return;
      // ── Success feedback ──────────────────────────────────────────
      final isEdit = widget.existing != null;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.check_circle_outline_rounded,
                  color: AppColors.surface, size: 20),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  isEdit
                      ? l10n.translate('changes_saved')
                      : l10n.translate('address_added'),
                  style: const TextStyle(
                    color: AppColors.surface,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          backgroundColor: AppColors.success,
          behavior: SnackBarBehavior.floating,
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          margin:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          duration: const Duration(seconds: 3),
        ),
      );
      if (widget.redirectOnSuccess != null) {
        context.go(widget.redirectOnSuccess!);
      } else {
        context.pop(true);
      }
    } on DioException catch (e) {
      // ── Decode backend error code for contextual messages ─────────
      final data = e.response?.data;
      final code =
          data is Map<String, dynamic> ? (data['error'] as String? ?? '') : '';
      String message;
      switch (code) {
        case 'DUPLICATE_ADDRESS_LABEL':
          message = l10n.translate('label_already_used_error');
          setState(() => _labelHasError = true);
          // Scroll to bottom so the label section is visible
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (_scrollCtrl.hasClients) {
              _scrollCtrl.animateTo(
                _scrollCtrl.position.maxScrollExtent,
                duration: const Duration(milliseconds: 450),
                curve: Curves.easeOut,
              );
            }
          });
          break;
        case 'CUSTOMER_NOT_FOUND':
          message = l10n.translate('account_not_found_error');
          break;
        case 'VALIDATION_ERROR':
          message = l10n.translate('invalid_fields_error');
          break;
        case 'INVALID_COORDINATES':
          message = l10n.translate('invalid_gps_error');
          break;
        default:
          message = AddressApiService.extractErrorMessage(e);
      }
      setState(() => _errorMessage = message);
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      backgroundColor: AppColors.surfaceLight,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded,
              color: AppColors.textPrimary),
          onPressed: () => context.pop(),
        ),
        title: Text(
          _screenTitle,
          style: const TextStyle(
            color: AppColors.textPrimary,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
        centerTitle: true,
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            controller: _scrollCtrl,
            padding: const EdgeInsets.all(20),
            children: [
              // Type header chip
              _TypeHeaderChip(
                icon: _typeIcon,
                label: _screenTitle,
                color: _typeColor,
              ),
              // ── Type selector (edit mode only) ──────────────────────
              if (widget.existing != null) ...[
                const SizedBox(height: 12),
                _SectionLabel(label: l10n.translate('address_type_label')),
                _TypeSelector(
                  currentType: _currentType,
                  onTypeChanged: _onTypeChanged,
                ),
              ],
              const SizedBox(height: 24),

              // ── Address field (read-only – détectée par GPS) ──────────
              _SectionLabel(label: l10n.translate('address_gps_section')),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                decoration: BoxDecoration(
                  color: AppColors.surfaceLight,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.softGrey),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.location_on_outlined,
                        color: AppColors.grey, size: 20),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        _addressCtrl.text.isEmpty
                            ? l10n.translate('address_not_available')
                            : _addressCtrl.text,
                        style: const TextStyle(
                          fontSize: 14,
                          color: AppColors.darkGrey,
                        ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const Icon(Icons.lock_outline_rounded,
                        color: AppColors.secondaryGrey, size: 16),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // ── Building name (APARTMENT only) ────────────────────────
              if (_currentType == AddressType.apartment) ...[
                _SectionLabel(label: l10n.translate('residence_name')),
                _AppTextField(
                  controller: _buildingCtrl,
                  hint: l10n.translate('residence_hint'),
                  icon: Icons.domain_rounded,
                  validator: (v) => (v == null || v.trim().isEmpty) ? l10n.translate('field_required') : null,
                ),
                const SizedBox(height: 16),
              ],

              // ── Floor + Door ──────────────────────────────────────────
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _SectionLabel(label: l10n.translate('floor_section')),
                        _AppTextField(
                          controller: _floorCtrl,
                          hint: l10n.translate('floor_hint'),
                          icon: Icons.stairs_rounded,
                          keyboardType: TextInputType.number,
                          validator: (v) => (v == null || v.trim().isEmpty) ? l10n.translate('field_required') : null,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _SectionLabel(label: l10n.translate('apartment_number')),
                        _AppTextField(
                          controller: _doorCtrl,
                          hint: l10n.translate('apartment_hint'),
                          icon: Icons.door_front_door_outlined,
                          validator: (v) => (v == null || v.trim().isEmpty) ? l10n.translate('field_required') : null,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // ── Extra info ────────────────────────────────────────────
              _SectionLabel(label: l10n.translate('extra_info')),
              _AppTextField(
                controller: _infoCtrl,
                hint: l10n.translate('extra_info_hint'),
                icon: Icons.info_outline_rounded,
                maxLines: 2,
              ),
              const SizedBox(height: 24),

              // ── Label ─────────────────────────────────────────────────
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Text(
                    l10n.translate('address_label_section'),
                    style: const TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: AppColors.darkGrey,
                    ),
                  ),
                  if (_labelHasError) ...[
                    const SizedBox(width: 6),
                    Icon(Icons.warning_amber_rounded,
                        color: Colors.orange.shade700, size: 16),
                    const SizedBox(width: 4),
                    Text(
                      l10n.translate('label_already_used_badge'),
                      style: TextStyle(
                          fontSize: 11,
                          color: Colors.orange.shade700,
                          fontWeight: FontWeight.w500),
                    ),
                  ],
                ],
              ),
              const SizedBox(height: 8),
              AnimatedContainer(
                duration: const Duration(milliseconds: 250),
                padding: _labelHasError
                    ? const EdgeInsets.all(10)
                    : EdgeInsets.zero,
                decoration: _labelHasError
                    ? BoxDecoration(
                        border:
                            Border.all(color: Colors.orange.shade400, width: 1.5),
                        borderRadius: BorderRadius.circular(14),
                        color: Colors.orange.shade50,
                      )
                    : const BoxDecoration(),
                child: _LabelSelector(
                  presets: _presetLabels,
                  selectedPreset: _selectedPresetLabel,
                  useCustom: _useCustomLabel,
                  customController: _customLabelCtrl,
                  color: _typeColor,
                  isOtherType: _currentType == AddressType.other,
                  onPresetSelected: (v) {
                    setState(() {
                      _selectedPresetLabel = v;
                      _useCustomLabel = false;
                      _labelHasError = false;
                      _errorMessage = null;
                    });
                  },
                  onCustomToggled: () {
                    setState(() {
                      _useCustomLabel = !_useCustomLabel;
                      if (_useCustomLabel) _selectedPresetLabel = null;
                      _labelHasError = false;
                      _errorMessage = null;
                    });
                  },
                ),
              ),
              const SizedBox(height: 24),

              // ── Error ─────────────────────────────────────────────────
              if (_errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 16, vertical: 12),
                  decoration: BoxDecoration(
                    color: Colors.red.shade50,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppColors.secondary),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.error_outline,
                          color: AppColors.error, size: 18),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: TextStyle(
                            color: AppColors.error,
                            fontSize: 13,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
              ],

              // ── Delete button (edit mode only) ──────────────────────
              if (widget.existing != null) ...[
                SizedBox(
                  height: 52,
                  child: OutlinedButton.icon(
                    onPressed: (_isLoading || _isDeleting) ? null : _delete,
                    icon: _isDeleting
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: AppColors.primary),
                          )
                        : const Icon(Icons.delete_outline_rounded,
                            color: AppColors.error, size: 20),
                    label: Text(
                      l10n.translate('delete_address'),
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: widget.existing!.isDefault
                            ? AppColors.primary
                            : AppColors.primary,
                      ),
                    ),
                    style: OutlinedButton.styleFrom(
                      side: BorderSide(
                        color: widget.existing!.isDefault
                            ? AppColors.secondary
                            : AppColors.secondaryDark,
                      ),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14)),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
              ],

              // ── Confirm button ────────────────────────────────────────
              SizedBox(
                height: 52,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _submit,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: AppColors.surface,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                    elevation: 0,
                  ),
                  child: _isLoading
                      ? const SizedBox(
                          width: 22,
                          height: 22,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.5,
                            color: AppColors.surface,
                          ),
                        )
                      : Text(
                          widget.existing != null
                              ? l10n.translate('save_changes')
                              : l10n.translate('confirm_address'),
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                ),
              ),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }
}

// ─── Sub-widgets ──────────────────────────────────────────────────────────────

class _TypeHeaderChip extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;

  const _TypeHeaderChip(
      {required this.icon, required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      decoration: BoxDecoration(
        color: color.withOpacity(0.10),
        borderRadius: BorderRadius.circular(30),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: color, size: 18),
          const SizedBox(width: 8),
          Text(label,
              style: TextStyle(
                  color: color,
                  fontWeight: FontWeight.w700,
                  fontSize: 14)),
        ],
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  final String label;

  const _SectionLabel({required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Text(
        label,
        style: const TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: AppColors.darkGrey,
        ),
      ),
    );
  }
}

class _AppTextField extends StatelessWidget {
  final TextEditingController controller;
  final String hint;
  final IconData icon;
  final int maxLines;
  final TextInputType? keyboardType;
  final String? Function(String?)? validator;

  const _AppTextField({
    required this.controller,
    required this.hint,
    required this.icon,
    this.maxLines = 1,
    this.keyboardType,
    this.validator,
  });

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      maxLines: maxLines,
      keyboardType: keyboardType,
      validator: validator,
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: const TextStyle(color: AppColors.secondaryGrey, fontSize: 14),
        prefixIcon: Icon(icon, color: AppColors.grey, size: 20),
        filled: true,
        fillColor: AppColors.surface,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.softGrey),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.softGrey),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: AppColors.primary, width: 1.6),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.error),
        ),
      ),
    );
  }
}

class _LabelSelector extends StatelessWidget {
  final List<String> presets;
  final String? selectedPreset;
  final bool useCustom;
  final bool isOtherType;
  final TextEditingController customController;
  final Color color;
  final ValueChanged<String> onPresetSelected;
  final VoidCallback onCustomToggled;

  const _LabelSelector({
    required this.presets,
    required this.selectedPreset,
    required this.useCustom,
    required this.customController,
    required this.color,
    required this.onPresetSelected,
    required this.onCustomToggled,
    this.isOtherType = false,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Preset chips (not shown for OTHER)
        if (!isOtherType)
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              ...presets.map((p) => _chip(p, p == selectedPreset && !useCustom,
                  onTap: () => onPresetSelected(p))),
              // "Personnalisé" chip
              _chip(
                l10n.translate('personalized'),
                useCustom,
                icon: Icons.edit_rounded,
                onTap: onCustomToggled,
              ),
            ],
          ),

        // Custom text field
        if (useCustom || isOtherType) ...[
          if (!isOtherType) const SizedBox(height: 12),
          TextFormField(
            controller: customController,
            decoration: InputDecoration(
              hintText: isOtherType
                  ? l10n.translate('custom_label_other_hint')
                  : l10n.translate('custom_label_hint'),
              hintStyle:
                  const TextStyle(color: AppColors.secondaryGrey, fontSize: 14),
              prefixIcon: const Icon(Icons.label_outline_rounded,
                  color: AppColors.grey, size: 20),
              filled: true,
              fillColor: AppColors.surface,
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.softGrey),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.softGrey),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: color, width: 1.6),
              ),
            ),
            validator: isOtherType
                ? (v) => (v == null || v.trim().isEmpty)
                    ? 'Label obligatoire pour ce type'
                    : null
                : null,
          ),
        ],
      ],
    );
  }

  Widget _chip(
    String label,
    bool selected, {
    IconData? icon,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? color : AppColors.surface,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: selected ? color : AppColors.softGrey,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(icon,
                  size: 14, color: selected ? AppColors.surface : color),
              const SizedBox(width: 4),
            ],
            Text(
              label,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: selected ? AppColors.surface : AppColors.darkGrey,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ─── Type selector (edit mode) ────────────────────────────────────────────────

class _TypeSelector extends StatelessWidget {
  final AddressType currentType;
  final ValueChanged<AddressType> onTypeChanged;

  const _TypeSelector({
    required this.currentType,
    required this.onTypeChanged,
  });

  static const _types = [
    (AddressType.home,      Icons.home_rounded,       'address_type_home',       AppColors.primary),
    (AddressType.work,      Icons.business_rounded,   'address_type_work',       AppColors.primaryDark),
    (AddressType.apartment, Icons.apartment_rounded,  'address_type_apartment',  AppColors.secondary),
    (AddressType.other,     Icons.location_on_rounded,'address_type_other',      AppColors.secondaryDark),
  ];

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: _types.map((entry) {
        final (type, icon, labelKey, color) = entry;
        final selected = type == currentType;
        return GestureDetector(
          onTap: () => onTypeChanged(type),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
            decoration: BoxDecoration(
              color: selected ? color : AppColors.surface,
              borderRadius: BorderRadius.circular(22),
              border: Border.all(
                color: selected ? color : AppColors.softGrey,
                width: selected ? 1.6 : 1.0,
              ),
              boxShadow: selected
                  ? [BoxShadow(color: color.withOpacity(0.18), blurRadius: 6)]
                  : null,
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(icon,
                    size: 16,
                    color: selected ? AppColors.surface : color),
                const SizedBox(width: 6),
                Text(
                  l10n.translate(labelKey),
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: selected ? AppColors.surface : AppColors.darkGrey,
                  ),
                ),
              ],
            ),
          ),
        );
      }).toList(),
    );
  }
}
