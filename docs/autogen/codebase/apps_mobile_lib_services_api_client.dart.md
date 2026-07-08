# 📄 ফাইল: apps/mobile/lib/services/api_client.dart

**প্রকার:** .dart  
**সাইজ:** 1,072 বাইট  
**আপডেট:** 2026-07-08T04:03:20.414760

---

## কোড

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter/foundation.dart';

class ApiClient {
  static const String baseUrl = 'https://api.supremeai.dev'; // আপনার প্রোডাকশন URL দিন

  // 1-Click Quick Actions (Rollback, Backup, Clear Cache)
  Future<bool> triggerQuickAction(String actionType) async {
    try {
      final response = await http.post(Uri.parse('$baseUrl/api/admin/actions/$actionType'));
      return response.statusCode == 200;
    } catch (e) {
      debugPrint('Action Trigger Error: $e');
      return false;
    }
  }

  // God Mode: Constitutional Rules
  Future<bool> updateGodRule(String key, bool value) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/admin/rules'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'key': key, 'value': value ? 'true' : 'false'}),
      );
      return response.statusCode == 200;
    } catch (e) {
      debugPrint('God Mode Error: $e');
      return false;
    }
  }
}

```