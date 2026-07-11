# 📄 ফাইল: apps/mobile/lib/src/theme/app_theme.dart

**প্রকার:** .dart  
**সাইজ:** 2,751 বাইট  
**আপডেট:** 2026-07-11T18:21:35.050385

---

## কোড

```dart
import 'package:flutter/material.dart';
import 'tokens.dart';

class SupremeThemeExtension extends ThemeExtension<SupremeThemeExtension> {
  final Color actionPrimaryBg;
  final Color actionPrimaryText;
  final Color brandPrimary;
  final Color brandSuccess;

  const SupremeThemeExtension({
    required this.actionPrimaryBg,
    required this.actionPrimaryText,
    required this.brandPrimary,
    required this.brandSuccess,
  });

  @override
  ThemeExtension<SupremeThemeExtension> copyWith({
    Color? actionPrimaryBg,
    Color? actionPrimaryText,
    Color? brandPrimary,
    Color? brandSuccess,
  }) {
    return SupremeThemeExtension(
      actionPrimaryBg: actionPrimaryBg ?? this.actionPrimaryBg,
      actionPrimaryText: actionPrimaryText ?? this.actionPrimaryText,
      brandPrimary: brandPrimary ?? this.brandPrimary,
      brandSuccess: brandSuccess ?? this.brandSuccess,
    );
  }

  @override
  ThemeExtension<SupremeThemeExtension> lerp(
      ThemeExtension<SupremeThemeExtension>? other, double t) {
    if (other is! SupremeThemeExtension) {
      return this;
    }
    return SupremeThemeExtension(
      actionPrimaryBg: Color.lerp(actionPrimaryBg, other.actionPrimaryBg, t)!,
      actionPrimaryText: Color.lerp(actionPrimaryText, other.actionPrimaryText, t)!,
      brandPrimary: Color.lerp(brandPrimary, other.brandPrimary, t)!,
      brandSuccess: Color.lerp(brandSuccess, other.brandSuccess, t)!,
    );
  }

  // Define light theme values using AppColors (Design Tokens)
  static final light = SupremeThemeExtension(
    actionPrimaryBg: AppColors.semanticColorActionPrimaryBg,
    actionPrimaryText: AppColors.semanticColorActionPrimaryText,
    brandPrimary: AppColors.colorBrand500,
    brandSuccess: const Color(0xFF10B981), // Mock value if not in tokens
  );

  // Define dark theme values
  static final dark = SupremeThemeExtension(
    actionPrimaryBg: AppColors.semanticColorActionPrimaryBg,
    actionPrimaryText: AppColors.semanticColorActionPrimaryText,
    brandPrimary: AppColors.colorBrand500,
    brandSuccess: const Color(0xFF10B981),
  );
}

class AppTheme {
  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.colorBrand500,
        brightness: Brightness.light,
        surface: AppColors.colorNeutral0,
      ),
      extensions: [SupremeThemeExtension.light],
    );
  }

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.colorBrand500,
        brightness: Brightness.dark,
        surface: AppColors.colorNeutral900,
      ),
      extensions: [SupremeThemeExtension.dark],
    );
  }
}

```