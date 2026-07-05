# 📄 ফাইল: apps/mobile/lib/theme/app_theme.dart

**প্রকার:** .dart  
**সাইজ:** 521 বাইট  
**আপডেট:** 2026-07-05T01:29:35.700980

---

## কোড

```dart
import 'package:flutter/material.dart';

class AppTheme {
  static ThemeData light = ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: Colors.deepPurple,
      brightness: Brightness.light,
    ),
    useMaterial3: true,
    scaffoldBackgroundColor: Colors.grey[50],
  );

  static ThemeData dark = ThemeData(
    colorScheme: ColorScheme.fromSeed(
      seedColor: Colors.deepPurple,
      brightness: Brightness.dark,
    ),
    useMaterial3: true,
    scaffoldBackgroundColor: Colors.grey[900],
  );
}
```