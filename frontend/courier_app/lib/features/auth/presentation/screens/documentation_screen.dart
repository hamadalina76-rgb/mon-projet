import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:dio/dio.dart';

import '../../../../config/runtime_config.dart';
import '../../../../core/localization/app_localizations.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/error/exceptions.dart';
import '../../../../config/di/injection_container.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/entities/courier.dart';

class DocumentationScreen extends StatefulWidget {
  final Courier? existingCourier;
  final bool readOnly;
  
  const DocumentationScreen({
    super.key,
    this.existingCourier,
    this.readOnly = false,
  });

  @override
  State<DocumentationScreen> createState() => _DocumentationScreenState();
}

class _DocumentationScreenState extends State<DocumentationScreen> {
  final _formKey = GlobalKey<FormState>();
  int _currentStep = 0;
  bool _isSubmitting = false;
  bool _isEditMode = false;
  bool _isLoading = true;
  Courier? _currentCourier;
  
  // Vehicle controllers
  final _vehicleModelController = TextEditingController();
  final _vehicleColorController = TextEditingController();
  final _plateNumberController = TextEditingController();
  String _selectedVehicleType = 'bicycle';
  
  // ID controllers
  final _idNumberController = TextEditingController();
  final ImagePicker _picker = ImagePicker();
  File? _uploadedIdFront;
  File? _uploadedIdBack;
  
  // License controllers
  final _licenseNumberController = TextEditingController();
  final _expiryDateController = TextEditingController();
  File? _uploadedLicenseFront;
  File? _uploadedLicenseBack;
  
  // Bank controllers
  final _accountHolderController = TextEditingController();
  final _accountNumberController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadCourierData();
  }

  Future<void> _loadCourierData() async {
    try {
      // First check if courier was passed as parameter
      if (widget.existingCourier != null) {
        _currentCourier = widget.existingCourier;
      } else {
        // Try to fetch latest courier profile from backend (/couriers/profile).
        if (!getIt.isRegistered<AuthRepository>()) {
          await setupDependencies();
        }
        final authRepository = getIt<AuthRepository>();
        try {
          _currentCourier = await authRepository.fetchCourierProfile();
        } catch (e) {
          // If remote fetch fails, fall back to locally saved courier data
          print('⚠️ Remote profile fetch failed, falling back to local: $e');
          _currentCourier = await authRepository.getCurrentCourier();
        }
      }

      // If we have courier data, populate the form
      if (_currentCourier != null) {
        // Treat any loaded courier as edit mode so user can update their documentation
        _isEditMode = true;
        _populateFormWithExistingData();
      }
    } catch (e) {
      print('⚠️ Could not load courier data: $e');
      // Continue with empty form for new documentation
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _populateFormWithExistingData() {
    if (_currentCourier == null) return;

    // Vehicle information
    if (_currentCourier!.vehicleType != null) {
      _selectedVehicleType = _mapBackendVehicleTypeToUi(_currentCourier!.vehicleType!);
    }
    if (_currentCourier!.vehicleModel != null) {
      _vehicleModelController.text = _currentCourier!.vehicleModel!;
    }
    if (_currentCourier!.vehicleColor != null) {
      _vehicleColorController.text = _currentCourier!.vehicleColor!;
    }
    if (_currentCourier!.vehicleNumber != null) {
      _plateNumberController.text = _currentCourier!.vehicleNumber!;
    }

    // ID information
    if (_currentCourier!.identityNumber != null) {
      _idNumberController.text = _currentCourier!.identityNumber!;
    }

    // License information
    if (_currentCourier!.drivingLicenseNumber != null) {
      _licenseNumberController.text = _currentCourier!.drivingLicenseNumber!;
    }
    if (_currentCourier!.drivingLicenseExpiry != null) {
      _expiryDateController.text = _currentCourier!.drivingLicenseExpiry!;
    }

    // Bank information
    if (_currentCourier!.bankAccountHolder != null) {
      _accountHolderController.text = _currentCourier!.bankAccountHolder!;
    }
    if (_currentCourier!.bankIban != null) {
      _accountNumberController.text = _currentCourier!.bankIban!;
    }

    print('✅ Form populated with existing data (Edit Mode: $_isEditMode)');
  }

  String _mapBackendVehicleTypeToUi(String backendType) {
    switch (backendType.toUpperCase()) {
      case 'ELECTRIC_BICYCLE':
        return 'ebike';
      case 'BICYCLE':
        return 'bicycle';
      case 'MOTORCYCLE':
        return 'motorcycle';
      case 'CAR':
        return 'car';
      case 'WALKING':
        return 'walking';
      default:
        return 'bicycle';
    }
  }

  String _mapUiVehicleTypeToBackend(String uiType) {
    switch (uiType) {
      case 'ebike':
        return 'ELECTRIC_BICYCLE';
      case 'bicycle':
        return 'BICYCLE';
      case 'motorcycle':
        return 'MOTORCYCLE';
      case 'car':
        return 'CAR';
      case 'walking':
        return 'WALKING';
      default:
        return 'BICYCLE';
    }
  }

  String? _toIsoDate(String input) {
    final trimmed = input.trim();
    if (trimmed.isEmpty) return null;
    final parts = trimmed.split('/');
    if (parts.length == 3) {
      final mm = parts[0].padLeft(2, '0');
      final dd = parts[1].padLeft(2, '0');
      final yyyy = parts[2];
      return '$yyyy-$mm-$dd';
    }
    // assume already ISO-like
    return trimmed;
  }

  @override
  void dispose() {
    _vehicleModelController.dispose();
    _vehicleColorController.dispose();
    _plateNumberController.dispose();
    _idNumberController.dispose();
    _licenseNumberController.dispose();
    _expiryDateController.dispose();
    _accountHolderController.dispose();
    _accountNumberController.dispose();
    super.dispose();
  }

  Future<void> _pickImage(bool isFront, {required bool isLicense}) async {
    final l10n = AppLocalizations.of(context)!;

    final source = await showDialog<ImageSource>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(
            l10n.translate('choose_source'),
            style: TextStyle(fontSize: 18.sp, fontWeight: FontWeight.bold),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ListTile(
                leading: Icon(Icons.photo_library, color: AppColors.primary, size: 28.sp),
                title: Text(l10n.translate('gallery'), style: TextStyle(fontSize: 16.sp)),
                onTap: () => Navigator.pop(context, ImageSource.gallery),
              ),
              ListTile(
                leading: Icon(Icons.camera_alt, color: AppColors.primary, size: 28.sp),
                title: Text(l10n.translate('camera'), style: TextStyle(fontSize: 16.sp)),
                onTap: () => Navigator.pop(context, ImageSource.camera),
              ),
            ],
          ),
        );
      },
    );

    if (source != null) {
      try {
        final XFile? image = await _picker.pickImage(
          source: source,
          maxWidth: 1800,
          maxHeight: 1800,
          imageQuality: 85,
        );
        
        if (image != null) {
          setState(() {
            if (isLicense) {
              if (isFront) {
                _uploadedLicenseFront = File(image.path);
              } else {
                _uploadedLicenseBack = File(image.path);
              }
            } else {
              if (isFront) {
                _uploadedIdFront = File(image.path);
              } else {
                _uploadedIdBack = File(image.path);
              }
            }
          });
        }
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${l10n.translate('image_pick_error')}: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  void _handleNext() async {
    final l10n = AppLocalizations.of(context)!;

    if (_formKey.currentState!.validate()) {
      // Validate based on current step
      if (_currentStep == 0) {
        // Vehicle step - just move forward
        setState(() {
          _currentStep++;
        });
      } else if (_currentStep == 1) {
        // ID Card step - check images (only required if not in edit mode or no existing images)
        final hasExistingImages = _isEditMode && 
                                  _currentCourier?.identityDocumentFrontImage != null &&
                                  _currentCourier?.identityDocumentBackImage != null;
        
        if (!hasExistingImages && (_uploadedIdFront == null || _uploadedIdBack == null)) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(l10n.translate('upload_both_id_sides')),
              backgroundColor: Colors.red,
            ),
          );
          return;
        }
        setState(() {
          _currentStep++;
        });
      } else if (_currentStep == 2) {
        // License step - check images (only required if not in edit mode or no existing image)
        final hasExistingLicense = _isEditMode && 
                                    _currentCourier?.drivingLicenseImage != null;
        
        if (!hasExistingLicense && _uploadedLicenseFront == null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(l10n.translate('upload_driving_license')),
              backgroundColor: Colors.red,
            ),
          );
          return;
        }
        setState(() {
          _currentStep++;
        });
      } else if (_currentStep == 3) {
        // Final step - submit documentation
        await _submitDocumentation();
      }
    }
  }

  Future<void> _submitDocumentation() async {
    final l10n = AppLocalizations.of(context)!;

    setState(() {
      _isSubmitting = true;
    });

    try {
      // Prepare documentation data
      final documentData = {
        'vehicleType': _selectedVehicleType,
        'vehicleModel': _vehicleModelController.text.trim(),
        'vehicleColor': _vehicleColorController.text.trim(),
        'plateNumber': _plateNumberController.text.trim(),
        'idNumber': _idNumberController.text.trim(),
        'licenseNumber': _licenseNumberController.text.trim(),
        'licenseExpiryDate': _expiryDateController.text.trim(),
        'accountHolder': _accountHolderController.text.trim(),
        'accountNumber': _accountNumberController.text.trim(),
        'documentationComplete': true,
      };

      // Prepare file paths
      final filePaths = <String, String>{};
      if (_uploadedIdFront != null) {
        filePaths['idCardFront'] = _uploadedIdFront!.path;
      }
      if (_uploadedIdBack != null) {
        filePaths['idCardBack'] = _uploadedIdBack!.path;
      }
      if (_uploadedLicenseFront != null) {
        filePaths['licenseFront'] = _uploadedLicenseFront!.path;
      }
      if (_uploadedLicenseBack != null) {
        filePaths['licenseBack'] = _uploadedLicenseBack!.path;
      }

      // Get auth repository
      if (!getIt.isRegistered<AuthRepository>()) {
        await setupDependencies();
      }
      final authRepository = getIt<AuthRepository>();

      if (_isEditMode) {
        // If files were picked in edit mode, upload them via multipart endpoint
        if (filePaths.isNotEmpty) {
          await authRepository.uploadDocumentation(
            documentData: documentData,
            filePaths: filePaths,
          );

          // Refresh local courier profile so UI shows new image URLs
          try {
            final refreshed = await authRepository.fetchCourierProfile();
            if (refreshed != null) {
              _currentCourier = refreshed;
            }
          } catch (e) {
            print('⚠️ Failed to refresh courier profile after upload: $e');
          }
        }

        // Update profile fields via /couriers/{id} for non-file fields
        final updateData = {
          'vehicleType': _mapUiVehicleTypeToBackend(_selectedVehicleType),
          'vehicleNumber': _plateNumberController.text.trim(),
          'vehicleModel': _vehicleModelController.text.trim(),
          'vehicleColor': _vehicleColorController.text.trim(),
          'identityNumber': _idNumberController.text.trim(),
          'drivingLicenseNumber': _licenseNumberController.text.trim(),
          'drivingLicenseExpiry': _toIsoDate(_expiryDateController.text),
          'bankAccountHolder': _accountHolderController.text.trim(),
          'bankIban': _accountNumberController.text.trim(),
        };

        updateData.removeWhere((key, value) => value == null || (value is String && value.isEmpty));
        await authRepository.updateProfile(data: updateData);
      } else {
        // New submission still uses multipart endpoint for document uploads
        await authRepository.uploadDocumentation(
          documentData: documentData,
          filePaths: filePaths,
        );
      }

      if (mounted) {
        // Show success dialog with appropriate message
        final successMessage = _isEditMode 
            ? l10n.translate('documentation_updated_success')
            : l10n.translate('documentation_submitted_success');
        
        await showDialog(
          context: context,
          barrierDismissible: false,
          builder: (BuildContext context) {
            return AlertDialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(15),
              ),
              title: Row(
                children: [
                  const Icon(Icons.check_circle, color: Colors.green, size: 28),
                  const SizedBox(width: 10),
                  Text(l10n.translate('success')),
                ],
              ),
              content: Text(
                successMessage,
                style: const TextStyle(fontSize: 16),
              ),
              actions: [
                ElevatedButton(
                  onPressed: () {
                    Navigator.of(context).pop();
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.green,
                    padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 12),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  child: Text(
                    l10n.translate('ok'),
                    style: TextStyle(fontSize: 16, color: Colors.white),
                  ),
                ),
              ],
            );
          },
        );
        
        // Navigate to home
        context.go('/home');
      }
    } catch (e) {
      print('❌ Documentation upload error: $e');
      String errorMessage = l10n.translate('generic_try_again_error');

      if (e is ApiValidationException) {
        errorMessage = e.userFriendlyMessage;
      } else if (e is DioException) {
        if (e.response != null && e.response?.data != null) {
          final data = e.response?.data;
          if (data is Map && data.containsKey('message')) {
            errorMessage = data['message']?.toString() ?? errorMessage;
          } else if (data is String) {
            errorMessage = data;
          } else {
            errorMessage = l10n.translate('server_error_try_later');
          }
        } else {
          errorMessage = e.message ?? l10n.translate('check_network_error');
        }
      } else {
        final s = e.toString();
        if (s.contains('Profile update failed') || s.contains('400')) {
          errorMessage = l10n.translate('check_entered_info_error');
        } else {
          errorMessage = s;
        }
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(errorMessage),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  Future<void> _selectExpiryDate() async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now().add(const Duration(days: 365)),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 3650)),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: ColorScheme.light(
              primary: AppColors.primary,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      setState(() {
        _expiryDateController.text =
            '${picked.month.toString().padLeft(2, '0')}/${picked.day.toString().padLeft(2, '0')}/${picked.year}';
      });
    }
  }
  
  void _handlePrevious() {
    if (_currentStep > 0) {
      setState(() {
        _currentStep--;
      });
    } else {
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    if (_isLoading) {
      return Scaffold(
        backgroundColor: Colors.white,
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(color: AppColors.primary),
              SizedBox(height: 16.h),
              Text(
                l10n.translate('loading_documentation'),
                style: TextStyle(
                  fontSize: 16.sp,
                  color: Colors.grey[600],
                ),
              ),
            ],
          ),
        ),
      );
    }

    final stepTitles = [
      l10n.translate('vehicle_information'),
      l10n.translate('id_card_details'),
      l10n.translate('license_details'),
      l10n.translate('bank_information'),
    ];
    
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: Colors.black, size: 24.sp),
          onPressed: _handlePrevious,
        ),
        title: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  widget.readOnly ? l10n.translate('documents_title') : stepTitles[_currentStep],
                  style: TextStyle(
                    fontSize: 20.sp,
                    fontWeight: FontWeight.bold,
                    color: Colors.black,
                  ),
                ),
              ],
            ),
            if (!widget.readOnly)
              Text(
                l10n.translateWithParams('step_of', {
                  'current': '${_currentStep + 1}',
                  'total': '4',
                }),
                style: TextStyle(
                  fontSize: 14.sp,
                  fontWeight: FontWeight.w400,
                  color: Colors.grey[600],
                ),
              )
            else
              Text(
                l10n.translate('read_only_mode'),
                style: TextStyle(
                  fontSize: 14.sp,
                  fontWeight: FontWeight.w400,
                  color: Colors.grey[600],
                ),
              ),
          ],
        ),
        centerTitle: true,
      ),
      body: Column(
        children: [
          // Progress Indicator
          if (!widget.readOnly) _buildProgressIndicator(),
          if (widget.readOnly)
            Container(
              width: double.infinity,
              margin: EdgeInsets.fromLTRB(24.w, 16.h, 24.w, 0),
              padding: EdgeInsets.all(12.w),
              decoration: BoxDecoration(
                color: Colors.blue[50],
                borderRadius: BorderRadius.circular(12.r),
                border: Border.all(color: Colors.blue[100]!),
              ),
              child: Row(
                children: [
                  Icon(Icons.visibility_outlined, color: Colors.blue[700], size: 20.sp),
                  SizedBox(width: 8.w),
                  Expanded(
                    child: Text(
                      l10n.translate('read_only_banner'),
                      style: TextStyle(
                        fontSize: 12.sp,
                        color: Colors.blue[900],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          
          // Content
          Expanded(
            child: SingleChildScrollView(
              padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 24.h),
              child: Form(
                key: _formKey,
                child: widget.readOnly
                    ? Column(
                        children: [
                          _buildVehicleStep(),
                          SizedBox(height: 24.h),
                          _buildIDCardStep(),
                          SizedBox(height: 24.h),
                          _buildLicenseStep(),
                          SizedBox(height: 24.h),
                          _buildBankStep(),
                        ],
                      )
                    : _buildCurrentStep(),
              ),
            ),
          ),
          
          // Bottom Navigation Buttons
          if (!widget.readOnly) _buildBottomNav(),
        ],
      ),
    );
  }

  Widget _buildProgressIndicator() {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 16.h),
      child: Row(
        children: List.generate(4, (index) {
          final isCompleted = index < _currentStep;
          final isCurrent = index == _currentStep;
          
          return Expanded(
            child: Container(
              height: 4.h,
              margin: EdgeInsets.symmetric(horizontal: 4.w),
              decoration: BoxDecoration(
                color: isCompleted || isCurrent
                    ? AppColors.primary
                    : Colors.grey[300],
                borderRadius: BorderRadius.circular(2.r),
              ),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildCurrentStep() {
    switch (_currentStep) {
      case 0:
        return _buildVehicleStep();
      case 1:
        return _buildIDCardStep();
      case 2:
        return _buildLicenseStep();
      case 3:
        return _buildBankStep();
      default:
        return Container();
    }
  }

  Widget _buildBottomNav() {
    final l10n = AppLocalizations.of(context)!;

    return Container(
      padding: EdgeInsets.all(24.w),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          width: double.infinity,
          height: 56.h,
          child: ElevatedButton(
            onPressed: _isSubmitting ? null : _handleNext,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(28.r),
              ),
              elevation: 2,
            ),
            child: _isSubmitting
                ? SizedBox(
                    height: 20.sp,
                    width: 20.sp,
                    child: const CircularProgressIndicator(
                      color: Colors.white,
                      strokeWidth: 2,
                    ),
                  )
                : Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        _currentStep == 3 
                          ? (_isEditMode
                            ? l10n.translate('update_upper')
                            : l10n.translate('complete_upper'))
                          : l10n.translate('next_upper'),
                        style: TextStyle(
                          fontSize: 16.sp,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                          letterSpacing: 0.5,
                        ),
                      ),
                      SizedBox(width: 8.w),
                      Icon(
                        _currentStep == 3 ? Icons.check_circle : Icons.arrow_forward,
                        size: 20.sp,
                      ),
                    ],
                  ),
          ),
        ),
      ),
    );
  }

  // Step 1: Vehicle Information
  Widget _buildVehicleStep() {
    final l10n = AppLocalizations.of(context)!;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: double.infinity,
          padding: EdgeInsets.all(24.w),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20.r),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.05),
                blurRadius: 20,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildSectionTitle(l10n.translate('vehicle_type')),
              SizedBox(height: 16.h),
              Column(
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: _buildVehicleTypeCard('bicycle', l10n.translate('vehicle_bicycle'), Icons.pedal_bike),
                      ),
                      SizedBox(width: 12.w),
                      Expanded(
                        child: _buildVehicleTypeCard('ebike', l10n.translate('vehicle_ebike'), Icons.electric_bike),
                      ),
                    ],
                  ),
                  SizedBox(height: 12.h),
                  Row(
                    children: [
                      Expanded(
                        child: _buildVehicleTypeCard('motorcycle', l10n.translate('vehicle_motorcycle'), Icons.two_wheeler),
                      ),
                      SizedBox(width: 12.w),
                      Expanded(
                        child: _buildVehicleTypeCard('car', l10n.translate('vehicle_car'), Icons.directions_car),
                      ),
                    ],
                  ),
                  SizedBox(height: 12.h),
                  _buildVehicleTypeCard('walking', l10n.translate('vehicle_walking'), Icons.directions_walk, fullWidth: true),
                ],
              ),
            ],
          ),
        ),
        
        SizedBox(height: 32.h),
        
        Container(
          width: double.infinity,
          padding: EdgeInsets.all(24.w),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20.r),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.05),
                blurRadius: 20,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildSectionTitle(l10n.translate('specifications')),
              SizedBox(height: 16.h),
              _buildTextField(
                controller: _vehicleModelController,
                label: l10n.translate('vehicle_model_label'),
                hint: l10n.translate('vehicle_model_hint'),
                icon: Icons.directions_car_outlined,
              ),
              SizedBox(height: 16.h),
              Row(
                children: [
                  Expanded(
                    child: _buildTextField(
                      controller: _vehicleColorController,
                      label: l10n.translate('color_label'),
                      hint: l10n.translate('color_hint'),
                      icon: Icons.palette_outlined,
                    ),
                  ),
                  SizedBox(width: 12.w),
                  Expanded(
                    child: _buildTextField(
                      controller: _plateNumberController,
                      label: l10n.translate('plate_label'),
                      hint: l10n.translate('plate_hint'),
                      icon: Icons.tag,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  // Step 2: ID Card Details
  Widget _buildIDCardStep() {
    final l10n = AppLocalizations.of(context)!;
    final hasExistingIdFront = _currentCourier?.identityDocumentFrontImage != null;
    final hasExistingIdBack = _currentCourier?.identityDocumentBackImage != null;
    
    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(24.w),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionTitle(l10n.translate('id_card_details')),
          SizedBox(height: 24.h),
          _buildTextField(
            controller: _idNumberController,
            label: l10n.translate('id_card_number_label'),
            hint: l10n.translate('id_card_number_hint'),
            icon: Icons.credit_card,
          ),
          SizedBox(height: 32.h),
          _buildSectionTitle(l10n.translate('upload_id_card')),
          if (_isEditMode && (hasExistingIdFront || hasExistingIdBack)) ...[
            SizedBox(height: 8.h),
            Container(
              padding: EdgeInsets.all(12.w),
              decoration: BoxDecoration(
                color: Colors.blue[50],
                borderRadius: BorderRadius.circular(8.r),
                border: Border.all(color: Colors.blue[200]!),
              ),
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.blue[700], size: 20.sp),
                  SizedBox(width: 8.w),
                  Expanded(
                    child: Text(
                      l10n.translate('documents_already_uploaded_replace'),
                      style: TextStyle(
                        fontSize: 12.sp,
                        color: Colors.blue[900],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
          SizedBox(height: 16.h),
          Row(
            children: [
              Expanded(
                child: _buildImageUpload(
                    label: l10n.translate('front_side'),
                  subtitle: hasExistingIdFront && _uploadedIdFront == null 
                      ? l10n.translate('already_uploaded_check')
                      : l10n.translate('clear_photo_front'),
                  imageFile: _uploadedIdFront,
                  onTap: widget.readOnly ? null : () => _pickImage(true, isLicense: false),
                  hasExisting: hasExistingIdFront && _uploadedIdFront == null,
                ),
              ),
              SizedBox(width: 16.w),
              Expanded(
                child: _buildImageUpload(
                    label: l10n.translate('back_side'),
                  subtitle: hasExistingIdBack && _uploadedIdBack == null 
                      ? l10n.translate('already_uploaded_check')
                      : l10n.translate('clear_photo_back'),
                  imageFile: _uploadedIdBack,
                  onTap: widget.readOnly ? null : () => _pickImage(false, isLicense: false),
                  hasExisting: hasExistingIdBack && _uploadedIdBack == null,
                ),
              ),
            ],
          ),
          if (hasExistingIdFront || hasExistingIdBack) ...[
            SizedBox(height: 16.h),
            Wrap(
              spacing: 10.w,
              runSpacing: 10.h,
              children: [
                if (hasExistingIdFront)
                  _buildViewDocumentButton(
                    label: l10n.translate('view_id_front'),
                    icon: Icons.visibility_outlined,
                    onTap: () => _openDocumentPreview(
                      title: l10n.translate('id_front_title'),
                      imageUrl: _currentCourier!.identityDocumentFrontImage!,
                    ),
                  ),
                if (hasExistingIdBack)
                  _buildViewDocumentButton(
                    label: l10n.translate('view_id_back'),
                    icon: Icons.visibility_outlined,
                    onTap: () => _openDocumentPreview(
                      title: l10n.translate('id_back_title'),
                      imageUrl: _currentCourier!.identityDocumentBackImage!,
                    ),
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  // Step 3: License Details  
  Widget _buildLicenseStep() {
    final l10n = AppLocalizations.of(context)!;
    final hasExistingLicense = _currentCourier?.drivingLicenseImage != null;
    
    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(24.w),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionTitle(l10n.translate('license_details')),
          SizedBox(height: 24.h),

          // License Number Field
          Text(
            'LICENSE NUMBER',
            style: TextStyle(
              fontSize: 12.sp,
              fontWeight: FontWeight.w600,
              color: Colors.grey[500],
              letterSpacing: 0.5,
            ),
          ),
          SizedBox(height: 8.h),
          TextFormField(
            controller: _licenseNumberController,
            readOnly: widget.readOnly,
            decoration: InputDecoration(
              hintText: l10n.translate('license_number_hint'),
              hintStyle: TextStyle(
                color: Colors.grey[400],
                fontSize: 16.sp,
              ),
              prefixIcon: Icon(
                Icons.badge,
                color: Colors.grey[600],
                size: 24.sp,
              ),
              filled: true,
              fillColor: Colors.grey[50],
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12.r),
                borderSide: BorderSide.none,
              ),
              contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 16.h),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter your license number';
              }
              return null;
            },
          ),
          SizedBox(height: 24.h),

          // License Expiry Date Field
          Text(
            'LICENSE EXPIRY DATE',
            style: TextStyle(
              fontSize: 12.sp,
              fontWeight: FontWeight.w600,
              color: Colors.grey[500],
              letterSpacing: 0.5,
            ),
          ),
          SizedBox(height: 8.h),
          TextFormField(
            controller: _expiryDateController,
            readOnly: true,
            onTap: widget.readOnly ? null : _selectExpiryDate,
            decoration: InputDecoration(
              hintText: l10n.translate('license_expiry_date_hint'),
              hintStyle: TextStyle(
                color: Colors.grey[400],
                fontSize: 16.sp,
              ),
              prefixIcon: Icon(
                Icons.calendar_today,
                color: Colors.grey[600],
                size: 20.sp,
              ),
              suffixIcon: Icon(
                Icons.calendar_month,
                color: Colors.grey[600],
                size: 24.sp,
              ),
              filled: true,
              fillColor: Colors.grey[50],
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12.r),
                borderSide: BorderSide.none,
              ),
              contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 16.h),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please select expiry date';
              }
              return null;
            },
          ),
          SizedBox(height: 32.h),

          // Document Photo Section
          Text(
            'DOCUMENT PHOTO',
            style: TextStyle(
              fontSize: 12.sp,
              fontWeight: FontWeight.w600,
              color: Colors.grey[500],
              letterSpacing: 0.5,
            ),
          ),
          if (_isEditMode && hasExistingLicense) ...[
            SizedBox(height: 8.h),
            Container(
              padding: EdgeInsets.all(12.w),
              decoration: BoxDecoration(
                color: Colors.blue[50],
                borderRadius: BorderRadius.circular(8.r),
                border: Border.all(color: Colors.blue[200]!),
              ),
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.blue[700], size: 20.sp),
                  SizedBox(width: 8.w),
                  Expanded(
                    child: Text(
                      l10n.translate('license_already_uploaded_replace'),
                      style: TextStyle(
                        fontSize: 12.sp,
                        color: Colors.blue[900],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
          SizedBox(height: 16.h),

          // Photo Upload Box
          GestureDetector(
            onTap: widget.readOnly ? null : () => _pickImage(true, isLicense: true),
            child: Container(
              width: double.infinity,
              height: 200.h,
              decoration: BoxDecoration(
                color: Colors.grey[50],
                borderRadius: BorderRadius.circular(16.r),
                border: Border.all(
                  color: _uploadedLicenseFront != null 
                      ? Colors.green[300]! 
                      : (hasExistingLicense && _uploadedLicenseFront == null)
                          ? Colors.blue[300]!
                          : Colors.grey[300]!,
                  width: 2,
                  style: BorderStyle.solid,
                ),
              ),
              child: _uploadedLicenseFront != null
                  ? ClipRRect(
                      borderRadius: BorderRadius.circular(14.r),
                      child: Stack(
                        fit: StackFit.expand,
                        children: [
                          Image.file(
                            _uploadedLicenseFront!,
                            fit: BoxFit.cover,
                          ),
                          Positioned(
                            top: 8.h,
                            right: 8.w,
                            child: Container(
                              padding: EdgeInsets.all(6.w),
                              decoration: BoxDecoration(
                                color: Colors.green[700],
                                shape: BoxShape.circle,
                              ),
                              child: Icon(
                                Icons.check,
                                color: Colors.white,
                                size: 20.sp,
                              ),
                            ),
                          ),
                        ],
                      ),
                    )
                  : Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          padding: EdgeInsets.all(16.w),
                          decoration: BoxDecoration(
                            color: hasExistingLicense 
                                ? Colors.blue[50] 
                                : Colors.red[50],
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            hasExistingLicense ? Icons.check_circle : Icons.add_a_photo,
                            color: hasExistingLicense 
                                ? Colors.blue[700]
                                : AppColors.primary,
                            size: 40.sp,
                          ),
                        ),
                        SizedBox(height: 16.h),
                        Text(
                          hasExistingLicense 
                              ? l10n.translate('already_uploaded')
                              : l10n.translate('upload_front_side'),
                          style: TextStyle(
                            fontSize: 18.sp,
                            fontWeight: FontWeight.w600,
                            color: hasExistingLicense 
                                ? Colors.blue[700]
                                : Colors.black,
                          ),
                        ),
                        SizedBox(height: 8.h),
                        Text(
                          hasExistingLicense 
                              ? l10n.translate('tap_to_replace')
                              : l10n.translate('clear_photo_no_glare'),
                          style: TextStyle(
                            fontSize: 12.sp,
                            color: Colors.grey[400],
                            letterSpacing: 0.5,
                          ),
                        ),
                      ],
                    ),
            ),
          ),
          if (hasExistingLicense) ...[
            SizedBox(height: 16.h),
            _buildViewDocumentButton(
              label: l10n.translate('view_license'),
              icon: Icons.visibility_outlined,
              onTap: () => _openDocumentPreview(
                title: l10n.translate('license_title'),
                imageUrl: _currentCourier!.drivingLicenseImage!,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildViewDocumentButton({
    required String label,
    required IconData icon,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12.r),
        child: Container(
          constraints: BoxConstraints(minWidth: 160.w),
          padding: EdgeInsets.symmetric(horizontal: 12.w, vertical: 12.h),
          decoration: BoxDecoration(
            color: AppColors.primary.withValues(alpha: 0.06),
            borderRadius: BorderRadius.circular(12.r),
            border: Border.all(color: AppColors.primary.withValues(alpha: 0.25)),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                padding: EdgeInsets.all(6.w),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(8.r),
                ),
                child: Icon(icon, size: 16.sp, color: AppColors.primary),
              ),
              SizedBox(width: 8.w),
              Expanded(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 13.sp,
                    fontWeight: FontWeight.w700,
                    color: AppColors.primary,
                  ),
                ),
              ),
              SizedBox(width: 6.w),
              Icon(Icons.chevron_right, size: 18.sp, color: AppColors.primary),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _openDocumentPreview({
    required String title,
    required String imageUrl,
  }) async {
    final l10n = AppLocalizations.of(context)!;
    final resolvedUrl = _resolveDocumentUrl(imageUrl);
    if (resolvedUrl == null) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('document_link_unavailable')),
        ),
      );
      return;
    }

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return Dialog(
          insetPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 24.h),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16.r),
          ),
          child: Padding(
            padding: EdgeInsets.all(14.w),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        title,
                        style: TextStyle(
                          fontSize: 16.sp,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    IconButton(
                      onPressed: () => Navigator.of(dialogContext).pop(),
                      icon: const Icon(Icons.close),
                    ),
                  ],
                ),
                SizedBox(height: 8.h),
                ClipRRect(
                  borderRadius: BorderRadius.circular(12.r),
                  child: SizedBox(
                    width: double.infinity,
                    height: 420.h,
                    child: InteractiveViewer(
                      minScale: 1,
                      maxScale: 4,
                      child: Image.network(
                        resolvedUrl,
                        fit: BoxFit.contain,
                        loadingBuilder: (context, child, loadingProgress) {
                          if (loadingProgress == null) return child;
                          return Center(
                            child: CircularProgressIndicator(
                              color: AppColors.primary,
                            ),
                          );
                        },
                        errorBuilder: (context, error, stackTrace) {
                          return Center(
                            child: Padding(
                              padding: EdgeInsets.all(16.w),
                              child: Text(
                                l10n.translate('document_load_error'),
                                textAlign: TextAlign.center,
                                style: TextStyle(
                                  fontSize: 14.sp,
                                  color: Colors.grey[700],
                                ),
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  String? _resolveDocumentUrl(String? rawUrl) {
    if (rawUrl == null || rawUrl.trim().isEmpty) {
      return null;
    }
    final trimmed = rawUrl.trim();
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
      return trimmed;
    }
    return '${RuntimeConfig.apiBaseUrl}$trimmed';
  }

  // Step 4: Bank Information
  Widget _buildBankStep() {
    final l10n = AppLocalizations.of(context)!;

    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(24.w),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            width: 80.w,
            height: 80.h,
            decoration: BoxDecoration(
              color: Colors.red[50],
              shape: BoxShape.circle,
            ),
            child: Icon(
              Icons.account_balance,
              size: 40.sp,
              color: AppColors.primary,
            ),
          ),
          SizedBox(height: 24.h),
          Text(
            l10n.translate('bank_information'),
            style: TextStyle(
              fontSize: 24.sp,
              fontWeight: FontWeight.bold,
              color: Colors.black,
            ),
          ),
          SizedBox(height: 12.h),
          Text(
            l10n.translate('bank_info_desc_short'),
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 15.sp,
              color: Colors.grey[600],
              height: 1.5,
            ),
          ),
          SizedBox(height: 32.h),
          _buildTextField(
            controller: _accountHolderController,
            label: l10n.translate('account_holder_name'),
            hint: l10n.translate('full_legal_name'),
            icon: Icons.person_outline,
          ),
          SizedBox(height: 24.h),
          _buildTextField(
            controller: _accountNumberController,
            label: l10n.translate('iban_account_number'),
            hint: l10n.translate('iban_hint'),
            icon: Icons.account_balance_wallet_outlined,
          ),
          SizedBox(height: 24.h),
          Container(
            padding: EdgeInsets.all(16.w),
            decoration: BoxDecoration(
              color: Colors.blue[50],
              borderRadius: BorderRadius.circular(12.r),
            ),
            child: Row(
              children: [
                Icon(Icons.lock, color: Colors.blue[700], size: 24.sp),
                SizedBox(width: 12.w),
                Expanded(
                  child: Text(
                    l10n.translate('bank_security_info_short'),
                    style: TextStyle(
                      fontSize: 13.sp,
                      color: Colors.blue[900],
                      height: 1.4,
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

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    required String hint,
    required IconData icon,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: 12.sp,
            fontWeight: FontWeight.w600,
            color: Colors.grey[600],
            letterSpacing: 0.5,
          ),
        ),
        SizedBox(height: 8.h),
        TextFormField(
          controller: controller,
          readOnly: widget.readOnly,
          style: TextStyle(fontSize: 16.sp),
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: TextStyle(color: Colors.grey[400]),
            prefixIcon: Icon(icon, color: Colors.grey[400]),
            filled: true,
            fillColor: Colors.grey[50],
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12.r),
              borderSide: BorderSide(color: Colors.grey[300]!),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12.r),
              borderSide: BorderSide(color: Colors.grey[300]!),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12.r),
              borderSide: BorderSide(color: AppColors.primary, width: 2),
            ),
            contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 16.h),
          ),
          validator: (value) => value?.isEmpty ?? true ? AppLocalizations.of(context)!.translate('required_field') : null,
        ),
      ],
    );
  }

  Widget _buildSectionTitle(String title) {
    return Container(
      decoration: BoxDecoration(
        border: Border(
          left: BorderSide(
            color: AppColors.primary,
            width: 4.w,
          ),
        ),
      ),
      padding: EdgeInsets.only(left: 12.w),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 20.sp,
          fontWeight: FontWeight.bold,
          color: Colors.black,
        ),
      ),
    );
  }

  Widget _buildImageUpload({
    required String label,
    required String subtitle,
    required File? imageFile,
    VoidCallback? onTap,
    bool hasExisting = false,
  }) {
    final isUploaded = imageFile != null;
    
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 160.h,
        decoration: BoxDecoration(
          color: Colors.grey[50],
          borderRadius: BorderRadius.circular(16.r),
          border: Border.all(
            color: isUploaded 
                ? Colors.green[300]! 
                : hasExisting 
                    ? Colors.blue[300]!
                    : Colors.grey[300]!,
            width: 2,
          ),
        ),
        child: imageFile != null
            ? ClipRRect(
                borderRadius: BorderRadius.circular(14.r),
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    Image.file(imageFile, fit: BoxFit.cover),
                    Positioned(
                      top: 8.h,
                      right: 8.w,
                      child: Container(
                        padding: EdgeInsets.all(4.w),
                        decoration: BoxDecoration(
                          color: Colors.green[700],
                          shape: BoxShape.circle,
                        ),
                        child: Icon(Icons.check, color: Colors.white, size: 16.sp),
                      ),
                    ),
                  ],
                ),
              )
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    padding: EdgeInsets.all(12.w),
                    decoration: BoxDecoration(
                      color: hasExisting ? Colors.blue[50] : Colors.red[50],
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      hasExisting ? Icons.check_circle : Icons.add_a_photo, 
                      color: hasExisting ? Colors.blue[700] : AppColors.primary, 
                      size: 32.sp,
                    ),
                  ),
                  SizedBox(height: 12.h),
                  Text(
                    label,
                    style: TextStyle(
                      fontSize: 16.sp, 
                      fontWeight: FontWeight.w600, 
                      color: hasExisting ? Colors.blue[700] : Colors.black,
                    ),
                  ),
                  SizedBox(height: 4.h),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: 12.sp, 
                      color: hasExisting ? Colors.blue[600] : Colors.grey[400],
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  Widget _buildPhotoUploadBox({
    required String label,
    required String subtitle,
    required bool isUploaded,
    required File? imageFile,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 160.h,
        decoration: BoxDecoration(
          color: Colors.grey[50],
          borderRadius: BorderRadius.circular(16.r),
          border: Border.all(
            color: isUploaded ? Colors.green[300]! : Colors.grey[300]!,
            width: 2,
            style: BorderStyle.solid,
          ),
        ),
        child: imageFile != null
            ? ClipRRect(
                borderRadius: BorderRadius.circular(14.r),
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    Image.file(
                      imageFile,
                      fit: BoxFit.cover,
                    ),
                    Positioned(
                      top: 8.h,
                      right: 8.w,
                      child: Container(
                        padding: EdgeInsets.all(4.w),
                        decoration: BoxDecoration(
                          color: Colors.green[700],
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          Icons.check,
                          color: Colors.white,
                          size: 16.sp,
                        ),
                      ),
                    ),
                  ],
                ),
              )
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    padding: EdgeInsets.all(12.w),
                    decoration: BoxDecoration(
                      color: Colors.red[50],
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.add_a_photo,
                      color: AppColors.primary,
                      size: 32.sp,
                    ),
                  ),
                  SizedBox(height: 12.h),
                  Text(
                    label,
                    style: TextStyle(
                      fontSize: 16.sp,
                      fontWeight: FontWeight.w600,
                      color: Colors.black,
                    ),
                  ),
                  SizedBox(height: 4.h),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: 12.sp,
                      color: Colors.grey[400],
                      letterSpacing: 0.5,
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  Widget _buildVehicleTypeCard(String type, String label, IconData icon, {bool fullWidth = false}) {
    final isSelected = _selectedVehicleType == type;
    
    return GestureDetector(
      onTap: widget.readOnly
          ? null
          : () {
              setState(() {
                _selectedVehicleType = type;
              });
            },
      child: Container(
        padding: EdgeInsets.symmetric(vertical: 20.h, horizontal: 16.w),
        decoration: BoxDecoration(
          color: isSelected ? AppColors.primary.withOpacity(0.05) : Colors.white,
          borderRadius: BorderRadius.circular(12.r),
          border: Border.all(
            color: isSelected ? AppColors.primary : Colors.grey[300]!,
            width: isSelected ? 2 : 1,
          ),
        ),
        child: fullWidth
            ? Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    icon,
                    color: isSelected ? AppColors.primary : Colors.grey[600],
                    size: 28.sp,
                  ),
                  SizedBox(width: 12.w),
                  Text(
                    label,
                    style: TextStyle(
                      fontSize: 16.sp,
                      fontWeight: FontWeight.w600,
                      color: isSelected ? AppColors.primary : Colors.grey[700],
                    ),
                  ),
                ],
              )
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    icon,
                    color: isSelected ? AppColors.primary : Colors.grey[600],
                    size: 32.sp,
                  ),
                  SizedBox(height: 8.h),
                  Text(
                    label,
                    style: TextStyle(
                      fontSize: 14.sp,
                      fontWeight: FontWeight.w600,
                      color: isSelected ? AppColors.primary : Colors.grey[700],
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
