import 'package:flutter/material.dart';

/// Utility class for responsive sizing and spacing
/// Provides helper methods to scale UI elements based on screen dimensions
class ResponsiveUtils {
  // Private constructor to prevent instantiation
  ResponsiveUtils._();

  /// Baseline dimensions (iPhone 8)
  static const double _baselineWidth = 375.0;
  static const double _baselineHeight = 667.0;

  /// Get responsive height based on screen size
  /// [context] - BuildContext to get screen dimensions
  /// [factor] - Percentage of screen height (0.0 - 1.0)
  /// Returns calculated height
  static double getResponsiveHeight(BuildContext context, double factor) {
    return MediaQuery.of(context).size.height * factor;
  }

  /// Get responsive width based on screen size
  /// [context] - BuildContext to get screen dimensions
  /// [factor] - Percentage of screen width (0.0 - 1.0)
  /// Returns calculated width
  static double getResponsiveWidth(BuildContext context, double factor) {
    return MediaQuery.of(context).size.width * factor;
  }

  /// Get responsive font size with scaling
  /// [context] - BuildContext to get screen dimensions
  /// [baseSize] - Base font size at 375px width
  /// Returns scaled font size (clamped between 0.8x and 1.3x)
  static double getResponsiveFontSize(BuildContext context, double baseSize) {
    double screenWidth = MediaQuery.of(context).size.width;
    // Scale factor based on screen width (375 is baseline iPhone)
    double scaleFactor = screenWidth / _baselineWidth;
    return baseSize * scaleFactor.clamp(0.8, 1.3);
  }

  /// Get responsive spacing with scaling
  /// [context] - BuildContext to get screen dimensions
  /// [baseSpacing] - Base spacing at 667px height
  /// Returns scaled spacing (clamped between 0.7x and 1.2x)
  static double getResponsiveSpacing(BuildContext context, double baseSpacing) {
    double screenHeight = MediaQuery.of(context).size.height;
    // Scale factor based on screen height (667 is baseline iPhone 8)
    double scaleFactor = screenHeight / _baselineHeight;
    return baseSpacing * scaleFactor.clamp(0.7, 1.2);
  }

  /// Get responsive size (for icons, avatars, etc.)
  /// [context] - BuildContext to get screen dimensions
  /// [baseSize] - Base size at 375px width
  /// Returns scaled size (clamped between 0.8x and 1.2x)
  static double getResponsiveSize(BuildContext context, double baseSize) {
    double screenWidth = MediaQuery.of(context).size.width;
    double scaleFactor = screenWidth / _baselineWidth;
    return baseSize * scaleFactor.clamp(0.8, 1.2);
  }

  /// Get responsive button height
  /// [context] - BuildContext to get screen dimensions
  /// [minHeight] - Minimum button height
  /// [maxHeight] - Maximum button height
  /// Returns button height between min and max
  static double getResponsiveButtonHeight(
    BuildContext context, {
    double minHeight = 48.0,
    double maxHeight = 60.0,
  }) {
    return getResponsiveHeight(context, 0.065).clamp(minHeight, maxHeight);
  }

  /// Check if device is a small phone (width < 360px)
  static bool isSmallPhone(BuildContext context) {
    return MediaQuery.of(context).size.width < 360;
  }

  /// Check if device is a tablet (width >= 600px)
  static bool isTablet(BuildContext context) {
    return MediaQuery.of(context).size.width >= 600;
  }

  /// Check if device is in landscape mode
  static bool isLandscape(BuildContext context) {
    return MediaQuery.of(context).orientation == Orientation.landscape;
  }

  /// Get screen width
  static double screenWidth(BuildContext context) {
    return MediaQuery.of(context).size.width;
  }

  /// Get screen height
  static double screenHeight(BuildContext context) {
    return MediaQuery.of(context).size.height;
  }
}
