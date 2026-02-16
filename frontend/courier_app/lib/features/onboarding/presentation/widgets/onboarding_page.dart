import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../../../core/theme/app_colors.dart';
import '../screens/onboarding_screen.dart';

class OnboardingPage extends StatelessWidget {
  final OnboardingPageData data;
  
  const OnboardingPage({
    super.key,
    required this.data,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: 24.w),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Illustration/Image placeholder
          SizedBox(
            height: 280.h,
            width: 280.w,
            child: Center(
              child: Image.asset(
                data.image,
                height: 280.h,
                width: 280.w,
                fit: BoxFit.contain,
                filterQuality: FilterQuality.high,
                isAntiAlias: true,
                errorBuilder: (context, error, stackTrace) {
                  // Fallback icon when image not found
                  return Icon(
                    Icons.delivery_dining,
                    size: 120.sp,
                    color: AppColors.primary,
                  );
                },
              ),
            ),
          ),
          
          SizedBox(height: 48.h),
          
          // Title
          Text(
            data.title,
            style: TextStyle(
              fontSize: 32.sp,
              fontWeight: FontWeight.w700,
              color: Colors.black87,
              height: 1.2,
            ),
            textAlign: TextAlign.center,
          ),
          
          SizedBox(height: 8.h),
          
          // Subtitle (highlighted)
          Text(
            data.subtitle,
            style: TextStyle(
              fontSize: 32.sp,
              fontWeight: FontWeight.w700,
              fontStyle: FontStyle.italic,
              color: AppColors.primary,
              height: 1.2,
            ),
            textAlign: TextAlign.center,
          ),
          
          SizedBox(height: 24.h),
          
          // Description
          Text(
            data.description,
            style: TextStyle(
              fontSize: 16.sp,
              color: AppColors.textSecondary,
              height: 1.5,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
