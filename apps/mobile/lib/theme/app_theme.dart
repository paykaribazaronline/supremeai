import 'package:flutter/material.dart';

class SupremeColors {
  // Dark Mode Colors
  static const brandPrimary = Color(0xFF00f3ff);   // Cyan Neon
  static const brandSecondary = Color(0xFFbc13fe);  // Electric Purple
  static const brandSuccess = Color(0xFF00ff66);    // Matrix Green
  static const brandWarning = Color(0xFFf59e0b);    // Amber
  static const brandDanger = Color(0xFFef4444);     // Red

  // Light Mode Colors
  static const brandPrimaryLight = Color(0xFF0284c7);
  static const brandSecondaryLight = Color(0xFF4f46e5);
  static const brandSuccessLight = Color(0xFF059669);
  static const brandWarningLight = Color(0xFFd97706);
  static const brandDangerLight = Color(0xFFdc2626);

  // Dark Mode Surface
  static const bgVoid = Color(0xFF030712);
  static const bgSurface = Color(0xFF111827);
  static const bgCard = Color(0x66111827);          // 40% opacity
  
  static const textPrimary = Color(0xFFf3f4f6);
  static const textMuted = Color(0xFF94a3b8);

  // Light Mode Surface
  static const bgVoidLight = Color(0xFFf0f9ff);
  static const bgSurfaceLight = Color(0xFFffffff);
  
  static const textPrimaryLight = Color(0xFF0f172a);
  static const textMutedLight = Color(0xFF475569);
}

class AppTheme {
  static ThemeData light = ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: SupremeColors.brandPrimaryLight,
      brightness: Brightness.light,
      primary: SupremeColors.brandPrimaryLight,
      secondary: SupremeColors.brandSecondaryLight,
    ),
    useMaterial3: true,
    scaffoldBackgroundColor: SupremeColors.bgVoidLight,
    textTheme: const TextTheme(
      bodyLarge: TextStyle(color: SupremeColors.textPrimaryLight),
      bodyMedium: TextStyle(color: SupremeColors.textPrimaryLight),
    ),
  );

  static ThemeData dark = ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: SupremeColors.brandPrimary,
      brightness: Brightness.dark,
      primary: SupremeColors.brandPrimary,
      secondary: SupremeColors.brandSecondary,
    ),
    useMaterial3: true,
    scaffoldBackgroundColor: SupremeColors.bgVoid,
    textTheme: const TextTheme(
      bodyLarge: TextStyle(color: SupremeColors.textPrimary),
      bodyMedium: TextStyle(color: SupremeColors.textPrimary),
    ),
  );
}