import 'package:flutter/material.dart';

/// Dark theme configuration for the app
class AppDarkTheme {
  static ThemeData get theme {
    return ThemeData(
      brightness: Brightness.dark,
      useMaterial3: true,
      
      // TODO: Configure dark theme colors
      // colorScheme: ColorScheme.dark(
      //   primary: AppColors.primary,
      //   secondary: AppColors.secondary,
      // ),
    );
  }
}
