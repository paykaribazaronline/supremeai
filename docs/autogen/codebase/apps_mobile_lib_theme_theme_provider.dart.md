# 📄 ফাইল: apps/mobile/lib/theme/theme_provider.dart

**প্রকার:** .dart  
**সাইজ:** 262 বাইট  
**আপডেট:** 2026-07-05T18:19:45.338026

---

## কোড

```dart
import 'package:flutter/material.dart';

class ThemeProvider with ChangeNotifier {
  ThemeMode _themeMode = ThemeMode.system;
  
  ThemeMode get themeMode => _themeMode;
  
  void setThemeMode(ThemeMode mode) {
    _themeMode = mode;
    notifyListeners();
  }
}
```