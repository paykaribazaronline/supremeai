# 📄 ফাইল: apps/mobile/lib/widgets/base_dashboard_scaffold.dart

**প্রকার:** .dart  
**সাইজ:** 862 বাইট  
**আপডেট:** 2026-07-03T13:02:02.907430

---

## কোড

```dart
import 'package:flutter/material.dart';
import '../../services/localization_service.dart';

class BaseDashboardScaffold extends StatelessWidget {
  final String titleKey;
  final Widget body;
  
  const BaseDashboardScaffold({
    super.key,
    required this.titleKey,
    required this.body,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        elevation: 0,
        title: Text(
          titleKey.tr(),
          style: const TextStyle(
            fontSize: 16, 
            fontWeight: FontWeight.w900, 
            letterSpacing: 1.5, 
            color: Colors.white
          )
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: body,
      ),
    );
  }
}

```