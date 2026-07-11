import 'package:flutter/material.dart';
import 'tokens.dart';

class AppTheme {
  static ThemeData light = ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: DesignTokens.colorBrandPrimaryLight,
      brightness: Brightness.light,
      primary: DesignTokens.colorBrandPrimaryLight,
      secondary: DesignTokens.colorBrandSecondaryLight,
    ),
    useMaterial3: true,
    scaffoldBackgroundColor: DesignTokens.colorBgVoidLight,
    textTheme: const TextTheme(
      // ফন্ট সাইজ টোকেনের নাম fontSize3xl থেকে fontSize_3xl এ আপডেট করা হয়েছে
      displayLarge: TextStyle(color: DesignTokens.colorTextPrimaryLight, fontSize: DesignTokens.fontSize_3xl, fontWeight: DesignTokens.fontWeightBold),
      headlineMedium: TextStyle(color: DesignTokens.colorTextPrimaryLight, fontSize: DesignTokens.fontSizeXl, fontWeight: DesignTokens.fontWeightSemibold),
      bodyLarge: TextStyle(color: DesignTokens.colorTextPrimaryLight, fontSize: DesignTokens.fontSizeBase, fontWeight: DesignTokens.fontWeightRegular),
      bodyMedium: TextStyle(color: DesignTokens.colorTextSecondaryLight, fontSize: DesignTokens.fontSizeSm, fontWeight: DesignTokens.fontWeightRegular),
      labelSmall: TextStyle(color: DesignTokens.colorTextDisabledLight, fontSize: DesignTokens.fontSizeXs, fontWeight: DesignTokens.fontWeightMedium),
    ),
  );

  static ThemeData dark = ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: DesignTokens.colorBrandPrimaryDark,
      brightness: Brightness.dark,
      primary: DesignTokens.colorBrandPrimaryDark,
      secondary: DesignTokens.colorBrandSecondaryDark,
    ),
    useMaterial3: true,
    scaffoldBackgroundColor: DesignTokens.colorBgVoidDark,
    textTheme: const TextTheme(
      // ডার্ক মোডের জন্যও ফন্ট সাইজের নাম আপডেট করা হলো
      displayLarge: TextStyle(color: DesignTokens.colorTextPrimaryDark, fontSize: DesignTokens.fontSize_3xl, fontWeight: DesignTokens.fontWeightBold),
      headlineMedium: TextStyle(color: DesignTokens.colorTextPrimaryDark, fontSize: DesignTokens.fontSizeXl, fontWeight: DesignTokens.fontWeightSemibold),
      bodyLarge: TextStyle(color: DesignTokens.colorTextPrimaryDark, fontSize: DesignTokens.fontSizeBase, fontWeight: DesignTokens.fontWeightRegular),
      bodyMedium: TextStyle(color: DesignTokens.colorTextSecondaryDark, fontSize: DesignTokens.fontSizeSm, fontWeight: DesignTokens.fontWeightRegular),
      labelSmall: TextStyle(color: DesignTokens.colorTextDisabledDark, fontSize: DesignTokens.fontSizeXs, fontWeight: DesignTokens.fontWeightMedium),
    ),
  );
}