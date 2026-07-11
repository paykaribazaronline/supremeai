import 'package:flutter/material.dart';

class SupremeColors {
  static const brandPrimary = Color(0xFF00f3ff);   // Cyan Neon
  static const brandSecondary = Color(0xFFbc13fe);  // Electric Purple
  static const brandSuccess = Color(0xFF00ff66);    // Matrix Green
  static const brandWarning = Color(0xFFf59e0b);    // Amber
  static const brandDanger = Color(0xFFef4444);     // Red

  // Dark Mode Surface
  static const bgVoid = Color(0xFF030712);
  static const bgSurface = Color(0xFF111827);
  static const bgCard = Color(0x66111827);          // 40% opacity
  
  static const textPrimary = Color(0xFFf3f4f6);
  static const textMuted = Color(0xFF94a3b8);
}

class AppTheme {
  static ThemeData light = ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: SupremeColors.brandPrimary,
      brightness: Brightness.light,
    ),
    useMaterial3: true,
    scaffoldBackgroundColor: Colors.grey[50],
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