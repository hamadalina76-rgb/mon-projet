import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../../core/localization/app_localizations.dart';
import '../../../../core/theme/app_colors.dart';

class DrivingLicenseScreen extends StatefulWidget {
  const DrivingLicenseScreen({super.key});

  @override
  State<DrivingLicenseScreen> createState() => _DrivingLicenseScreenState();
}

class _DrivingLicenseScreenState extends State<DrivingLicenseScreen> {
  final _formKey = GlobalKey<FormState>();
  final _licenseNumberController = TextEditingController();
  final _expiryDateController = TextEditingController();
  final ImagePicker _picker = ImagePicker();
  File? _uploadedLicenseFront;

  @override
  void dispose() {
    _licenseNumberController.dispose();
    _expiryDateController.dispose();
    super.dispose();
  }

  Future<void> _selectDate() async {
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

  Future<void> _pickImage() async {
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
            _uploadedLicenseFront = File(image.path);
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

  void _handleContinue() {
    final l10n = AppLocalizations.of(context)!;

    if (_formKey.currentState!.validate()) {
      if (_uploadedLicenseFront == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.translate('upload_license_photo')),
            backgroundColor: Colors.red,
          ),
        );
        return;
      }
      // Navigate to payout details screen
      context.push('/payout-details');
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: Colors.black, size: 24.sp),
          onPressed: () => context.pop(),
        ),
        title: Column(
          children: [
            Text(
              l10n.translate('driving_license_step_title'),
              style: TextStyle(
                fontSize: 20.sp,
                fontWeight: FontWeight.bold,
                color: Colors.black,
              ),
            ),
            Text(
              l10n.translate('driving_license_subtitle'),
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
      body: SingleChildScrollView(
        padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 24.h),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // License Details Section
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
                    _buildSectionTitle(l10n.translate('license_details')),
                    SizedBox(height: 24.h),

                    // License Number Field
                    Text(
                      l10n.translate('license_number_label'),
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
                          return l10n.translate('enter_license_number');
                        }
                        return null;
                      },
                    ),
                    SizedBox(height: 24.h),

                    // License Expiry Date Field
                    Text(
                      l10n.translate('license_expiry_date_label'),
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
                      onTap: _selectDate,
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
                          return l10n.translate('select_expiry_date');
                        }
                        return null;
                      },
                    ),
                    SizedBox(height: 32.h),

                    // Document Photo Section
                    Text(
                      l10n.translate('document_photo'),
                      style: TextStyle(
                        fontSize: 12.sp,
                        fontWeight: FontWeight.w600,
                        color: Colors.grey[500],
                        letterSpacing: 0.5,
                      ),
                    ),
                    SizedBox(height: 16.h),

                    // Photo Upload Box
                    GestureDetector(
                      onTap: _pickImage,
                      child: Container(
                        width: double.infinity,
                        height: 200.h,
                        decoration: BoxDecoration(
                          color: Colors.grey[50],
                          borderRadius: BorderRadius.circular(16.r),
                          border: Border.all(
                            color: _uploadedLicenseFront != null ? Colors.green[300]! : Colors.grey[300]!,
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
                                      color: Colors.red[50],
                                      shape: BoxShape.circle,
                                    ),
                                    child: Icon(
                                      Icons.add_a_photo,
                                      color: AppColors.primary,
                                      size: 40.sp,
                                    ),
                                  ),
                                  SizedBox(height: 16.h),
                                  Text(
                                    l10n.translate('upload_front_side'),
                                    style: TextStyle(
                                      fontSize: 18.sp,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.black,
                                    ),
                                  ),
                                  SizedBox(height: 8.h),
                                  Text(
                                    l10n.translate('clear_photo_no_glare'),
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
                  ],
                ),
              ),
              SizedBox(height: 48.h),

              // Continue Button
              SizedBox(
                width: double.infinity,
                height: 56.h,
                child: ElevatedButton(
                  onPressed: _handleContinue,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(28.r),
                    ),
                    elevation: 2,
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        l10n.translate('next_upper'),
                        style: TextStyle(
                          fontSize: 16.sp,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                          letterSpacing: 0.5,
                        ),
                      ),
                      SizedBox(width: 8.w),
                      Icon(Icons.arrow_forward, size: 20.sp),
                    ],
                  ),
                ),
              ),
              SizedBox(height: 24.h),

              // Progress Indicator
              Center(
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          width: 32.w,
                          height: 4.h,
                          decoration: BoxDecoration(
                            color: AppColors.primary,
                            borderRadius: BorderRadius.circular(2.r),
                          ),
                        ),
                        SizedBox(width: 8.w),
                        Container(
                          width: 96.w,
                          height: 4.h,
                          decoration: BoxDecoration(
                            color: Colors.grey[300],
                            borderRadius: BorderRadius.circular(2.r),
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 16.h),
                    Text(
                      l10n.translate('driving_license_progress'),
                      style: TextStyle(
                        fontSize: 11.sp,
                        color: Colors.grey[400],
                        letterSpacing: 0.5,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
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
}
