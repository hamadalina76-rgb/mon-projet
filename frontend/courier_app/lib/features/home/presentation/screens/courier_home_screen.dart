import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

import '../../../../core/theme/app_colors.dart';

class CourierHomeScreen extends StatelessWidget {
  const CourierHomeScreen({super.key, this.canAccessApp = true});

  final bool canAccessApp;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: EdgeInsets.all(24.w),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Welcome Back!',
                          style: TextStyle(
                            fontSize: 16.sp,
                            color: Colors.grey[600],
                          ),
                        ),
                        SizedBox(height: 4.h),
                        Text(
                          'Ready to deliver?',
                          style: TextStyle(
                            fontSize: 24.sp,
                            fontWeight: FontWeight.bold,
                            color: Colors.black,
                          ),
                        ),
                      ],
                    ),
                    Container(
                      padding: EdgeInsets.all(12.w),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(12.r),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.05),
                            blurRadius: 10,
                            offset: const Offset(0, 2),
                          ),
                        ],
                      ),
                      child: Icon(
                        Icons.notifications_outlined,
                        color: AppColors.primary,
                        size: 24.sp,
                      ),
                    ),
                  ],
                ),
                if (!canAccessApp) ...[
                  SizedBox(height: 16.h),
                  Container(
                    width: double.infinity,
                    padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 12.h),
                    decoration: BoxDecoration(
                      color: Colors.orange.shade50,
                      borderRadius: BorderRadius.circular(12.r),
                      border: Border.all(color: Colors.orange.shade200),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.info_outline, color: Colors.orange.shade700, size: 24.sp),
                        SizedBox(width: 12.w),
                        Expanded(
                          child: Text(
                            'Votre compte est en cours d\'examen. Seul votre profil est accessible. Les notifications vous indiqueront quand votre compte sera activé.',
                            style: TextStyle(fontSize: 13.sp, color: Colors.orange.shade900),
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 16.h),
                ],
                SizedBox(height: 24.h),
                // Online Status Card
                Opacity(
                  opacity: canAccessApp ? 1 : 0.5,
                  child: IgnorePointer(
                    ignoring: !canAccessApp,
                    child: Container(
                      width: double.infinity,
                      padding: EdgeInsets.all(20.w),
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [AppColors.primary, AppColors.primary.withOpacity(0.7)],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                        borderRadius: BorderRadius.circular(20.r),
                        boxShadow: [
                          BoxShadow(
                            color: AppColors.primary.withOpacity(0.3),
                            blurRadius: 15,
                            offset: const Offset(0, 5),
                          ),
                        ],
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'You are Offline',
                                style: TextStyle(
                                  fontSize: 20.sp,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                              ),
                              SizedBox(height: 8.h),
                              Text(
                                'Go online to receive orders',
                                style: TextStyle(
                                  fontSize: 14.sp,
                                  color: Colors.white.withOpacity(0.9),
                                ),
                              ),
                            ],
                          ),
                          Container(
                            width: 60.w,
                            height: 32.h,
                            decoration: BoxDecoration(
                              color: Colors.white.withOpacity(0.3),
                              borderRadius: BorderRadius.circular(20.r),
                            ),
                            child: Switch(
                              value: false,
                              onChanged: canAccessApp ? (value) {} : null,
                              activeColor: Colors.green,
                              inactiveThumbColor: Colors.white,
                              inactiveTrackColor: Colors.transparent,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                SizedBox(height: 32.h),
                // Today's Summary
                Opacity(
                  opacity: canAccessApp ? 1 : 0.5,
                  child: IgnorePointer(ignoring: !canAccessApp, child: _buildSummarySection(context)),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSummarySection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
                  'Today\'s Summary',
                  style: TextStyle(
                    fontSize: 20.sp,
                    fontWeight: FontWeight.bold,
                    color: Colors.black,
                  ),
                ),
                
                SizedBox(height: 16.h),
                
                // Stats Grid
                Row(
                  children: [
                    Expanded(
                      child: _buildStatCard(
                        icon: Icons.receipt_long,
                        title: 'Orders',
                        value: '0',
                        color: Colors.blue,
                      ),
                    ),
                    SizedBox(width: 12.w),
                    Expanded(
                      child: _buildStatCard(
                        icon: Icons.attach_money,
                        title: 'Earnings',
                        value: '\$0',
                        color: Colors.green,
                      ),
                    ),
                  ],
                ),
                
                SizedBox(height: 12.h),
                
                Row(
                  children: [
                    Expanded(
                      child: _buildStatCard(
                        icon: Icons.schedule,
                        title: 'Online',
                        value: '0h',
                        color: Colors.orange,
                      ),
                    ),
                    SizedBox(width: 12.w),
                    Expanded(
                      child: _buildStatCard(
                        icon: Icons.local_shipping,
                        title: 'Distance',
                        value: '0km',
                        color: Colors.purple,
                      ),
                    ),
                  ],
                ),
                
                SizedBox(height: 32.h),
                
                // Quick Actions
                Text(
                  'Quick Actions',
                  style: TextStyle(
                    fontSize: 20.sp,
                    fontWeight: FontWeight.bold,
                    color: Colors.black,
                  ),
                ),
                
                SizedBox(height: 16.h),
                
                _buildQuickAction(
                  icon: Icons.history,
                  title: 'Order History',
                  subtitle: 'View your past deliveries',
                  onTap: () {
                    // TODO: Navigate to order history
                  },
                ),
                
                SizedBox(height: 12.h),
                
                _buildQuickAction(
                  icon: Icons.bar_chart,
                  title: 'Earnings Report',
                  subtitle: 'Check your earnings details',
                  onTap: () {
                    // TODO: Navigate to earnings
                  },
                ),
                
                SizedBox(height: 12.h),
                
                _buildQuickAction(
                  icon: Icons.support_agent,
                  title: 'Contact Support',
                  subtitle: 'Get help with your issues',
                  onTap: () {
                    // TODO: Navigate to support
                  },
                ),
      ],
    );
  }

  Widget _buildStatCard({
    required IconData icon,
    required String title,
    required String value,
    required Color color,
  }) {
    return Container(
      padding: EdgeInsets.all(16.w),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        children: [
          Icon(
            icon,
            size: 32.sp,
            color: color,
          ),
          SizedBox(height: 8.h),
          Text(
            value,
            style: TextStyle(
              fontSize: 20.sp,
              fontWeight: FontWeight.bold,
              color: Colors.black,
            ),
          ),
          SizedBox(height: 4.h),
          Text(
            title,
            style: TextStyle(
              fontSize: 12.sp,
              color: Colors.grey[600],
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildQuickAction({
    required IconData icon,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16.r),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: ListTile(
        onTap: onTap,
        contentPadding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.h),
        leading: Container(
          width: 48.w,
          height: 48.h,
          decoration: BoxDecoration(
            color: AppColors.primary.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12.r),
          ),
          child: Icon(
            icon,
            color: AppColors.primary,
            size: 24.sp,
          ),
        ),
        title: Text(
          title,
          style: TextStyle(
            fontSize: 16.sp,
            fontWeight: FontWeight.w600,
            color: Colors.black,
          ),
        ),
        subtitle: Padding(
          padding: EdgeInsets.only(top: 4.h),
          child: Text(
            subtitle,
            style: TextStyle(
              fontSize: 14.sp,
              color: Colors.grey[600],
            ),
          ),
        ),
        trailing: Icon(
          Icons.arrow_forward_ios,
          size: 16.sp,
          color: Colors.grey[400],
        ),
      ),
    );
  }
}
