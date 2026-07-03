# 📄 ফাইল: apps/mobile/lib/theme/theme_provider.dart

**প্রকার:** .dart  
**সাইজ:** 262 বাইট  
**আপডেট:** 2026-07-03T15:24:11.629346

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