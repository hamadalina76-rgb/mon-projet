import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/theme/app_colors.dart';

class PayoutDetailsScreen extends StatefulWidget {
  const PayoutDetailsScreen({super.key});

  @override
  State<PayoutDetailsScreen> createState() => _PayoutDetailsScreenState();
}

class _PayoutDetailsScreenState extends State<PayoutDetailsScreen> {
  final _formKey = GlobalKey<FormState>();
  final _accountHolderController = TextEditingController();
  final _accountNumberController = TextEditingController();

  @override
  void dispose() {
    _accountHolderController.dispose();
    _accountNumberController.dispose();
    super.dispose();
  }

  void _handleComplete() {
    if (_formKey.currentState!.validate()) {
      // TODO: Submit all registration data
      // Show success message and navigate to home
      context.go('/home');
    }
  }

  @override
  Widget build(BuildContext context) {
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
              'Payout Details',
              style: TextStyle(
                fontSize: 20.sp,
                fontWeight: FontWeight.bold,
                color: Colors.black,
              ),
            ),
            Text(
              'Step 4 of 4',
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
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Bank Information Card
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
                  children: [
                    // Bank Icon
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

                    // Title and Description
                    Text(
                      'Bank Information',
                      style: TextStyle(
                        fontSize: 24.sp,
                        fontWeight: FontWeight.bold,
                        color: Colors.black,
                      ),
                    ),
                    SizedBox(height: 12.h),
                    Text(
                      'Please provide your bank details to\nreceive your weekly payouts.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 15.sp,
                        color: Colors.grey[600],
                        height: 1.5,
                      ),
                    ),
                    SizedBox(height: 32.h),

                    // Account Holder Name Field
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                        'ACCOUNT HOLDER NAME',
                        style: TextStyle(
                          fontSize: 12.sp,
                          fontWeight: FontWeight.w600,
                          color: Colors.grey[500],
                          letterSpacing: 0.5,
                        ),
                      ),
                    ),
                    SizedBox(height: 8.h),
                    TextFormField(
                      controller: _accountHolderController,
                      decoration: InputDecoration(
                        hintText: 'Full Legal Name',
                        hintStyle: TextStyle(
                          color: Colors.grey[400],
                          fontSize: 16.sp,
                        ),
                        prefixIcon: Icon(
                          Icons.person_outline,
                          color: Colors.grey[400],
                          size: 24.sp,
                        ),
                        filled: true,
                        fillColor: Colors.grey[50],
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12.r),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 16.w,
                          vertical: 16.h,
                        ),
                      ),
                      validator: (value) {
                        if (value == null || value.isEmpty) {
                          return 'Please enter account holder name';
                        }
                        return null;
                      },
                    ),
                    SizedBox(height: 24.h),

                    // IBAN / Account Number Field
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                        'IBAN / ACCOUNT NUMBER',
                        style: TextStyle(
                          fontSize: 12.sp,
                          fontWeight: FontWeight.w600,
                          color: Colors.grey[500],
                          letterSpacing: 0.5,
                        ),
                      ),
                    ),
                    SizedBox(height: 8.h),
                    TextFormField(
                      controller: _accountNumberController,
                      decoration: InputDecoration(
                        hintText: 'FR76 0000 0000 0000...',
                        hintStyle: TextStyle(
                          color: Colors.grey[400],
                          fontSize: 16.sp,
                        ),
                        prefixIcon: Icon(
                          Icons.credit_card,
                          color: Colors.grey[400],
                          size: 24.sp,
                        ),
                        filled: true,
                        fillColor: Colors.grey[50],
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12.r),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 16.w,
                          vertical: 16.h,
                        ),
                      ),
                      validator: (value) {
                        if (value == null || value.isEmpty) {
                          return 'Please enter your account number';
                        }
                        return null;
                      },
                    ),
                    SizedBox(height: 24.h),

                    // Security Info Box
                    Container(
                      padding: EdgeInsets.all(16.w),
                      decoration: BoxDecoration(
                        color: Colors.blue[50],
                        borderRadius: BorderRadius.circular(12.r),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            Icons.lock,
                            color: Colors.blue[700],
                            size: 24.sp,
                          ),
                          SizedBox(width: 12.w),
                          Expanded(
                            child: Text(
                              'Your banking information is encrypted and securely stored. We only use this for automated payout processing.',
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
              ),
              SizedBox(height: 32.h),

              // Complete Registration Button
              SizedBox(
                width: double.infinity,
                height: 56.h,
                child: ElevatedButton(
                  onPressed: _handleComplete,
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
                        'CREATE ACCOUNT',
                        style: TextStyle(
                          fontSize: 16.sp,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                          letterSpacing: 0.5,
                        ),
                      ),
                      SizedBox(width: 8.w),
                      Icon(Icons.check_circle, size: 20.sp),
                    ],
                  ),
                ),
              ),
              SizedBox(height: 24.h),

              // Terms Text
              RichText(
                textAlign: TextAlign.center,
                text: TextSpan(
                  style: TextStyle(
                    fontSize: 13.sp,
                    color: Colors.grey[600],
                    height: 1.5,
                  ),
                  children: [
                    const TextSpan(
                      text: 'By completing registration, you confirm that the\nbank details provided belong to you. ',
                    ),
                    TextSpan(
                      text: 'Terms of\nService',
                      style: TextStyle(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w600,
                        decoration: TextDecoration.underline,
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(height: 24.h),

              // Progress Indicator
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    width: 80.w,
                    height: 4.h,
                    decoration: BoxDecoration(
                      color: Colors.grey[300],
                      borderRadius: BorderRadius.circular(2.r),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
