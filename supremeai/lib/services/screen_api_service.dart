import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;

class ScreenApiService {
  static const String _baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'https://supremeai-a.web.app',
  );

  static String get baseUrl {
    final base = _baseUrl.replaceAll(RegExp(r'/+$'), '');
    return base;
  }

  Future<String?> _getAuthToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('auth_token');
  }

  Future<Map<String, String>> _getAuthHeaders() async {
    final token = await _getAuthToken();
    final headers = <String, String>{'Content-Type': 'application/json'};
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  Future<Map<String, dynamic>> ping(String screen) async {
    final headers = await _getAuthHeaders();
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/screens/$screen/status'),
        headers: headers,
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        return json.decode(response.body) as Map<String, dynamic>;
      }
    } catch (_) {
      // fall through to local fallback
    }
    return _localFallback(screen);
  }

  Future<Map<String, dynamic>> action(String screen, Map<String, dynamic> payload) async {
    final headers = await _getAuthHeaders();
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/screens/$screen/action'),
        headers: headers,
        body: json.encode(payload),
      ).timeout(const Duration(seconds: 15));
      return json.decode(response.body) as Map<String, dynamic>;
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  Map<String, dynamic> _localFallback(String screen) {
    return {
      'status': 'ok',
      'mode': 'local',
      'screen': screen,
      'message': 'Backend unavailable — using local context.',
      'timestamp': DateTime.now().toIso8601String(),
      'data': <String, dynamic>{},
    };
  }
}
