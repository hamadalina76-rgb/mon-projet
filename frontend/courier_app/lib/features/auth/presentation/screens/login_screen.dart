import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';
import 'package:dio/dio.dart';

import '../../../../core/theme/app_colors.dart';
import '../../../../config/di/injection_container.dart';
import '../../domain/repositories/auth_repository.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _phoneController = TextEditingController();
  bool _isPasswordVisible = false;
  bool _isLoginTab = true;
  bool _acceptTerms = false;
  bool _isLoading = false;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _firstNameController.dispose();
    _lastNameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  void _handleSignIn() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isLoading = true;
      });
      
      try {
        print('🔍 Checking if AuthRepository is registered: ${getIt.isRegistered<AuthRepository>()}');
        
        // If not registered, set up dependencies again
        if (!getIt.isRegistered<AuthRepository>()) {
          print('⚠️ AuthRepository not found, setting up dependencies...');
          await setupDependencies();
        }
        
        final authRepository = getIt<AuthRepository>();
        print('✅ Got AuthRepository instance');
        final courier = await authRepository.login(
          email: _emailController.text.trim(),
          password: _passwordController.text,
        );
        
        print('📋 Login response - courier ID: ${courier.id}, documentsVerified: ${courier.documentsVerified}');
        
        if (mounted) {
          // Check if documents are verified
          if (!courier.documentsVerified) {
            print('⚠️ Documents not verified, redirecting to documentation screen');
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Please complete your documentation to continue'),
                backgroundColor: Colors.orange,
                duration: Duration(seconds: 3),
              ),
            );
            context.go('/documentation');
          } else {
            print('✅ Documents verified, redirecting to home');
            context.go('/home');
          }
        }
      } catch (e) {
        print('❌ Login error: $e');
        String errorMessage = 'Login failed';
        
        if (e is DioException) {
          if (e.response != null && e.response?.data != null) {
            // Extract error message from backend
            final data = e.response?.data;
            if (data is Map && data.containsKey('message')) {
              errorMessage = data['message'];
            } else if (data is String) {
              errorMessage = data;
            } else {
              errorMessage = 'Status ${e.response?.statusCode}: ${e.message}';
            }
          } else {
            errorMessage = e.message ?? 'Network error';
          }
        } else {
          errorMessage = e.toString();
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
            _isLoading = false;
          });
        }
      }
    }
  }

  void _handleSignUp() async {
    if (_formKey.currentState!.validate()) {
      if (!_acceptTerms) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please accept the Terms and Conditions')),
        );
        return;
      }
      
      setState(() {
        _isLoading = true;
      });
      
      try {
        print('🔍 Checking if AuthRepository is registered: ${getIt.isRegistered<AuthRepository>()}');
        
        // If not registered, set up dependencies again
        if (!getIt.isRegistered<AuthRepository>()) {
          print('⚠️ AuthRepository not found, setting up dependencies...');
          await setupDependencies();
        }
        
        final authRepository = getIt<AuthRepository>();
        print('✅ Got AuthRepository instance');
        
        // Ensure phone number has proper format
        String phoneNumber = _phoneController.text.trim().replaceAll(' ', '');
        if (!phoneNumber.startsWith('+')) {
          // Remove leading zero if present
          if (phoneNumber.startsWith('0')) {
            phoneNumber = phoneNumber.substring(1);
          }
          phoneNumber = '+216$phoneNumber'; // Add Tunisia country code
        }
        
        final registrationData = {
          'email': _emailController.text.trim(),
          'password': _passwordController.text,
          'firstName': _firstNameController.text.trim(),
          'lastName': _lastNameController.text.trim(),
          'phoneNumber': phoneNumber,
          'role': 'COURIER',
        };
        
        print('📤 Sending registration data: ${registrationData.map((k, v) => MapEntry(k, k == "password" ? "***" : v))}');
        
        await authRepository.register(data: registrationData);
        
        if (mounted) {
          // Show success dialog
          await showDialog(
            context: context,
            barrierDismissible: false,
            builder: (BuildContext context) {
              return AlertDialog(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(15),
                ),
                title: const Row(
                  children: [
                    Icon(Icons.check_circle, color: Colors.green, size: 28),
                    SizedBox(width: 10),
                    Text('Success'),
                  ],
                ),
                content: const Text(
                  'Account created successfully!',
                  style: TextStyle(fontSize: 16),
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
                    child: const Text(
                      'OK',
                      style: TextStyle(fontSize: 16, color: Colors.white),
                    ),
                  ),
                ],
              );
            },
          );
          
          // Switch to login tab
          setState(() {
            _isLoginTab = true;
          });
        }
      } catch (e) {
        print('❌ Registration error: $e');
        String errorMessage = 'Registration failed';
        
        if (e is DioException) {
          print('📋 Response status: ${e.response?.statusCode}');
          print('📋 Response data: ${e.response?.data}');
          
          if (e.response != null && e.response?.data != null) {
            // Extract error message from backend
            final data = e.response?.data;
            if (data is Map && data.containsKey('message')) {
              errorMessage = data['message'];
            } else if (data is Map && data.containsKey('error')) {
              errorMessage = data['error'];
            } else if (data is String) {
              errorMessage = data;
            } else {
              errorMessage = 'Status ${e.response?.statusCode}: ${e.message}';
            }
          } else {
            errorMessage = e.message ?? 'Network error';
          }
        } else {
          errorMessage = e.toString();
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
            _isLoading = false;
          });
        }
      }
    }
  }

  void _handleForgotPassword() {
    // TODO: Navigate to forgot password screen
  }

  void _handleContactSupport() {
    // TODO: Open support contact
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: 24.w),
            child: Column(
              children: [
                SizedBox(height: 40.h),
                
                // Logo
                Image.asset(
                  'assets/images/speedline_logo.png',
                  height: 100.h,
                  filterQuality: FilterQuality.high,
                  isAntiAlias: true,
                  errorBuilder: (context, error, stackTrace) {
                    return Column(
                      children: [
                        Icon(
                          Icons.delivery_dining,
                          size: 60.sp,
                          color: AppColors.primary,
                        ),
                        SizedBox(height: 8.h),
                        Text(
                          'SpeedLine',
                          style: TextStyle(
                            fontSize: 24.sp,
                            fontWeight: FontWeight.bold,
                            color: AppColors.primary,
                          ),
                        ),
                      ],
                    );
                  },
                ),
                
                SizedBox(height: 16.h),
                
                // Tagline
                Text(
                  'Fast delivery, right to your door.',
                  style: TextStyle(
                    fontSize: 16.sp,
                    color: Colors.grey[600],
                  ),
                  textAlign: TextAlign.center,
                ),
                
                SizedBox(height: 32.h),
                
                // Login/Signup Card
                Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20.r),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.05),
                        blurRadius: 10,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Padding(
                    padding: EdgeInsets.all(24.w),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Tabs
                          Row(
                            children: [
                              Expanded(
                                child: GestureDetector(
                                  onTap: () {
                                    setState(() {
                                      _isLoginTab = true;
                                    });
                                  },
                                  child: Container(
                                    padding: EdgeInsets.symmetric(vertical: 12.h),
                                    decoration: BoxDecoration(
                                      color: _isLoginTab ? Colors.white : Colors.grey[100],
                                      borderRadius: BorderRadius.circular(8.r),
                                    ),
                                    child: Text(
                                      'Log In',
                                      style: TextStyle(
                                        fontSize: 16.sp,
                                        fontWeight: FontWeight.w600,
                                        color: _isLoginTab ? AppColors.primary : Colors.grey[600],
                                      ),
                                      textAlign: TextAlign.center,
                                    ),
                                  ),
                                ),
                              ),
                              SizedBox(width: 12.w),
                              Expanded(
                                child: GestureDetector(
                                  onTap: () {
                                    setState(() {
                                      _isLoginTab = false;
                                    });
                                  },
                                  child: Container(
                                    padding: EdgeInsets.symmetric(vertical: 12.h),
                                    decoration: BoxDecoration(
                                      color: !_isLoginTab ? Colors.white : Colors.grey[100],
                                      borderRadius: BorderRadius.circular(8.r),
                                    ),
                                    child: Text(
                                      'Sign Up',
                                      style: TextStyle(
                                        fontSize: 16.sp,
                                        fontWeight: FontWeight.w600,
                                        color: !_isLoginTab ? AppColors.primary : Colors.grey[600],
                                      ),
                                      textAlign: TextAlign.center,
                                    ),
                                  ),
                                ),
                              ),
                            ],
                          ),
                          
                          SizedBox(height: 24.h),
                          
                          // Show Login Form or Signup Form
                          if (_isLoginTab) ...[
                            // LOGIN FORM
                            // Email Address Label
                            Text(
                              'Email Address',
                              style: TextStyle(
                                fontSize: 16.sp,
                                fontWeight: FontWeight.w600,
                                color: Colors.black,
                              ),
                            ),
                            
                            SizedBox(height: 8.h),
                            
                            // Email Field
                            TextFormField(
                              controller: _emailController,
                              keyboardType: TextInputType.emailAddress,
                              style: TextStyle(fontSize: 16.sp, color: Colors.black),
                              decoration: InputDecoration(
                                hintText: 'you@example.com',
                                hintStyle: TextStyle(
                                  fontSize: 16.sp,
                                  color: Colors.grey[400],
                                ),
                                prefixIcon: Icon(
                                  Icons.email_outlined,
                                  color: Colors.grey[400],
                                  size: 24.sp,
                                ),
                                filled: true,
                                fillColor: Colors.grey[50],
                                border: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12.r),
                                  borderSide: BorderSide(color: Colors.grey[300]!, width: 1),
                                ),
                                enabledBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12.r),
                                  borderSide: BorderSide(color: Colors.grey[300]!, width: 1),
                                ),
                                focusedBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12.r),
                                  borderSide: BorderSide(color: AppColors.primary, width: 2),
                                ),
                                contentPadding: EdgeInsets.symmetric(
                                  horizontal: 16.w,
                                  vertical: 16.h,
                                ),
                              ),
                              validator: (value) {
                                if (value == null || value.isEmpty) {
                                  return 'Please enter your email';
                                }
                                if (!value.contains('@')) {
                                  return 'Please enter a valid email';
                                }
                                return null;
                              },
                            ),
                            
                            SizedBox(height: 20.h),
                            
                            // Password Label with Forgot Password
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  'Password',
                                  style: TextStyle(
                                    fontSize: 16.sp,
                                    fontWeight: FontWeight.w600,
                                    color: Colors.black,
                                  ),
                                ),
                                GestureDetector(
                                  onTap: _handleForgotPassword,
                                  child: Text(
                                    'Forgot Password?',
                                    style: TextStyle(
                                      fontSize: 14.sp,
                                      color: AppColors.primary,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            
                            SizedBox(height: 8.h),
                            
                            // Password Field
                            TextFormField(
                              controller: _passwordController,
                              obscureText: !_isPasswordVisible,
                              style: TextStyle(fontSize: 16.sp, color: Colors.black),
                              decoration: InputDecoration(
                                hintText: '••••••••',
                                hintStyle: TextStyle(
                                  fontSize: 20.sp,
                                  color: Colors.grey[400],
                                  letterSpacing: 4,
                                ),
                                prefixIcon: Icon(
                                  Icons.lock_outline,
                                  color: Colors.grey[400],
                                  size: 24.sp,
                                ),
                                suffixIcon: IconButton(
                                  icon: Icon(
                                    _isPasswordVisible
                                        ? Icons.visibility_outlined
                                        : Icons.visibility_off_outlined,
                                    color: Colors.grey[400],
                                    size: 24.sp,
                                  ),
                                  onPressed: () {
                                    setState(() {
                                      _isPasswordVisible = !_isPasswordVisible;
                                    });
                                  },
                                ),
                                filled: true,
                                fillColor: Colors.grey[50],
                                border: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12.r),
                                  borderSide: BorderSide(color: Colors.grey[300]!, width: 1),
                                ),
                                enabledBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12.r),
                                  borderSide: BorderSide(color: Colors.grey[300]!, width: 1),
                                ),
                                focusedBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12.r),
                                  borderSide: BorderSide(color: AppColors.primary, width: 2),
                                ),
                                contentPadding: EdgeInsets.symmetric(
                                  horizontal: 16.w,
                                  vertical: 16.h,
                                ),
                              ),
                              validator: (value) {
                                if (value == null || value.isEmpty) {
                                  return 'Please enter your password';
                                }
                                return null;
                              },
                            ),
                            
                            SizedBox(height: 24.h),
                            
                            // Sign In Button
                            SizedBox(
                              width: double.infinity,
                              height: 56.h,
                              child: ElevatedButton(
                                onPressed: _isLoading ? null : _handleSignIn,
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: AppColors.primary,
                                  disabledBackgroundColor: Colors.grey[400],
                                  foregroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(28.r),
                                  ),
                                  elevation: 2,
                                ),
                                child: _isLoading
                                    ? SizedBox(
                                        width: 24.sp,
                                        height: 24.sp,
                                        child: const CircularProgressIndicator(
                                          color: Colors.white,
                                          strokeWidth: 2,
                                        ),
                                      )
                                    : Row(
                                        mainAxisAlignment: MainAxisAlignment.center,
                                        children: [
                                          Text(
                                            'Sign In',
                                            style: TextStyle(
                                              fontSize: 18.sp,
                                              fontWeight: FontWeight.w600,
                                            ),
                                          ),
                                          SizedBox(width: 8.w),
                                          Icon(Icons.arrow_forward, size: 20.sp),
                                        ],
                                      ),
                              ),
                            ),
                          ] else ...[
                            // SIGNUP FORM
                            // First Name and Last Name Row
                            Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'Prénom',
                                        style: TextStyle(
                                          fontSize: 16.sp,
                                          fontWeight: FontWeight.w600,
                                          color: Colors.black,
                                        ),
                                      ),
                                      SizedBox(height: 8.h),
                                      TextFormField(
                                        controller: _firstNameController,
                                        style: TextStyle(fontSize: 16.sp),
                                        decoration: InputDecoration(
                                          hintText: 'John',
                                          hintStyle: TextStyle(color: Colors.grey[400]),
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
                                        validator: (value) => value?.isEmpty ?? true ? 'Required' : null,
                                      ),
                                    ],
                                  ),
                                ),
                                SizedBox(width: 12.w),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'Nom',
                                        style: TextStyle(
                                          fontSize: 16.sp,
                                          fontWeight: FontWeight.w600,
                                          color: Colors.black,
                                        ),
                                      ),
                                      SizedBox(height: 8.h),
                                      TextFormField(
                                        controller: _lastNameController,
                                        style: TextStyle(fontSize: 16.sp),
                                        decoration: InputDecoration(
                                          hintText: 'Doe',
                                          hintStyle: TextStyle(color: Colors.grey[400]),
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
                                        validator: (value) => value?.isEmpty ?? true ? 'Required' : null,
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            
                            SizedBox(height: 20.h),
                            
                            // Email
                            Text(
                              'Email Address',
                              style: TextStyle(
                                fontSize: 16.sp,
                                fontWeight: FontWeight.w600,
                                color: Colors.black,
                              ),
                            ),
                            SizedBox(height: 8.h),
                            TextFormField(
                              controller: _emailController,
                              keyboardType: TextInputType.emailAddress,
                              style: TextStyle(fontSize: 16.sp),
                              decoration: InputDecoration(
                                hintText: 'john.doe@example.com',
                                hintStyle: TextStyle(color: Colors.grey[400]),
                                prefixIcon: Icon(Icons.email_outlined, color: Colors.grey[400]),
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
                              validator: (value) {
                                if (value?.isEmpty ?? true) return 'Email is required';
                                if (!value!.contains('@') || !value.contains('.')) return 'Invalid email format';
                                return null;
                              },
                            ),
                            
                            SizedBox(height: 20.h),

                            // Password
                            Text(
                              'Password',
                              style: TextStyle(
                                fontSize: 16.sp,
                                fontWeight: FontWeight.w600,
                                color: Colors.black,
                              ),
                            ),
                            SizedBox(height: 8.h),
                            TextFormField(
                              controller: _passwordController,
                              obscureText: !_isPasswordVisible,
                              style: TextStyle(fontSize: 16.sp, color: Colors.black),
                              decoration: InputDecoration(
                                hintText: '••••••••',
                                hintStyle: TextStyle(
                                  fontSize: 20.sp,
                                  color: Colors.grey[400],
                                  letterSpacing: 4,
                                ),
                                prefixIcon: Icon(
                                  Icons.lock_outline,
                                  color: Colors.grey[400],
                                  size: 24.sp,
                                ),
                                suffixIcon: IconButton(
                                  icon: Icon(
                                    _isPasswordVisible
                                        ? Icons.visibility_outlined
                                        : Icons.visibility_off_outlined,
                                    color: Colors.grey[400],
                                    size: 24.sp,
                                  ),
                                  onPressed: () {
                                    setState(() {
                                      _isPasswordVisible = !_isPasswordVisible;
                                    });
                                  },
                                ),
                                filled: true,
                                fillColor: Colors.grey[50],
                                border: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12.r),
                                  borderSide: BorderSide(color: Colors.grey[300]!, width: 1),
                                ),
                                enabledBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12.r),
                                  borderSide: BorderSide(color: Colors.grey[300]!, width: 1),
                                ),
                                focusedBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12.r),
                                  borderSide: BorderSide(color: AppColors.primary, width: 2),
                                ),
                                contentPadding: EdgeInsets.symmetric(
                                  horizontal: 16.w,
                                  vertical: 16.h,
                                ),
                              ),
                              validator: (value) {
                                if (value == null || value.isEmpty) {
                                  return 'Please enter your password';
                                }
                                if (value.length < 8) {
                                  return 'Password must be at least 8 characters';
                                }
                                return null;
                              },
                            ),
                            
                            // Phone Number
                            Text(
                              'Phone Number',
                              style: TextStyle(
                                fontSize: 16.sp,
                                fontWeight: FontWeight.w600,
                                color: Colors.black,
                              ),
                            ),
                            SizedBox(height: 8.h),
                            TextFormField(
                              controller: _phoneController,
                              keyboardType: TextInputType.phone,
                              style: TextStyle(fontSize: 16.sp),
                              decoration: InputDecoration(
                                hintText: '+216 20202020',
                                hintStyle: TextStyle(color: Colors.grey[400]),
                                prefixIcon: Icon(Icons.phone_outlined, color: Colors.grey[400]),
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
                              validator: (value) {
                                if (value == null || value.isEmpty) {
                                  return 'Phone number is required';
                                }
                                // Remove spaces and check if it's a valid phone number
                                final cleaned = value.replaceAll(' ', '');
                                if (!cleaned.startsWith('+216') && cleaned.length < 8) {
                                  return 'Invalid phone number';
                                }
                                return null;
                              },
                            ),
                            SizedBox(height: 20.h),
                            
                            // Terms and Conditions Checkbox
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Checkbox(
                                  value: _acceptTerms,
                                  onChanged: (value) {
                                    setState(() {
                                      _acceptTerms = value ?? false;
                                    });
                                  },
                                  activeColor: AppColors.primary,
                                ),
                                Expanded(
                                  child: Padding(
                                    padding: EdgeInsets.only(top: 12.h),
                                    child: RichText(
                                      text: TextSpan(
                                        text: 'I accept the ',
                                        style: TextStyle(fontSize: 14.sp, color: Colors.grey[600]),
                                        children: [
                                          TextSpan(
                                            text: 'Terms and Conditions (CGU)',
                                            style: TextStyle(color: AppColors.primary, fontWeight: FontWeight.w600),
                                          ),
                                          TextSpan(text: ' and Privacy Policy.'),
                                        ],
                                      ),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            
                            SizedBox(height: 24.h),
                            
                            // Next Button
                            SizedBox(
                              width: double.infinity,
                              height: 56.h,
                              child: ElevatedButton(
                                onPressed: _isLoading ? null : _handleSignUp,
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: AppColors.primary,
                                  disabledBackgroundColor: Colors.grey[400],
                                  foregroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(28.r),
                                  ),
                                  elevation: 2,
                                ),
                                child: _isLoading
                                    ? SizedBox(
                                        width: 24.sp,
                                        height: 24.sp,
                                        child: const CircularProgressIndicator(
                                          color: Colors.white,
                                          strokeWidth: 2,
                                        ),
                                      )
                                    : Row(
                                        mainAxisAlignment: MainAxisAlignment.center,
                                        children: [
                                          Text(
                                            'CREATE ACCOUNT',
                                            style: TextStyle(
                                              fontSize: 18.sp,
                                              fontWeight: FontWeight.w600,
                                            ),
                                          ),
                                          SizedBox(width: 8.w),
                                          Icon(Icons.check_circle, size: 20.sp),
                                        ],
                                      ),
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                ),
                
                SizedBox(height: 32.h),
                
                // Bottom text
                if (_isLoginTab) ...[
                  // Contact Support
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        'Having trouble? ',
                        style: TextStyle(
                          fontSize: 14.sp,
                          color: Colors.grey[600],
                        ),
                      ),
                      GestureDetector(
                        onTap: _handleContactSupport,
                        child: Text(
                          'Contact Support',
                          style: TextStyle(
                            fontSize: 14.sp,
                            color: AppColors.primary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                ] else ...[
                  // Already have account
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        'Already have an account? ',
                        style: TextStyle(
                          fontSize: 14.sp,
                          color: Colors.grey[600],
                        ),
                      ),
                      GestureDetector(
                        onTap: () {
                          setState(() {
                            _isLoginTab = true;
                          });
                        },
                        child: Text(
                          'Sign In',
                          style: TextStyle(
                            fontSize: 14.sp,
                            color: AppColors.primary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
                
                SizedBox(height: 24.h),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
