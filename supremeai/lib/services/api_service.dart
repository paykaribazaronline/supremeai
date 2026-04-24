import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;

class ApiService {
  static const String _baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080',
  );

  String? _token;
  final http.Client client;

  ApiService({http.Client? client}) : client = client ?? http.Client();

  Future<String?> getToken() async {
    if (_token != null) return _token;
    final prefs = await SharedPreferences.getInstance();
    _token = prefs.getString('auth_token');
    return _token;
  }

  Future<Map<String, dynamic>> loginWithFirebase(String idToken) async {
    try {
      final response = await client.post(
        Uri.parse('$_baseUrl/api/auth/firebase-login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'idToken': idToken}),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        if (data['status'] == 'success') {
          _token = data['token'] ?? data['idToken'];
          if (_token != null) {
            final prefs = await SharedPreferences.getInstance();
            await prefs.setString('auth_token', _token!);
          }
          return {'success': true, 'data': data};
        }
      }
      return {
        'success': false,
        'error': 'Login failed (${response.statusCode})'
      };
    } catch (e) {
      return {'success': false, 'error': 'Connection error: $e'};
    }
  }

  Future<Map<String, dynamic>> register(
      String email, String password, String displayName) async {
    try {
      final response = await client.post(
        Uri.parse('$_baseUrl/api/auth/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email,
          'password': password,
          'displayName': displayName,
        }),
      );

      if (response.statusCode == 201) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return {'success': true, 'data': data};
      }
      return {
        'success': false,
        'error': 'Registration failed (${response.statusCode})'
      };
    } catch (e) {
      return {'success': false, 'error': 'Connection error: $e'};
    }
  }

  Future<Map<String, dynamic>> getUserProfile() async {
    try {
      final token = await getToken();
      final response = await client.get(
        Uri.parse('$_baseUrl/api/auth/profile'),
        headers: {
          'Content-Type': 'application/json',
          if (token != null) 'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        return {'success': true, 'data': jsonDecode(response.body)};
      }
      return {'success': false, 'error': 'Failed to load profile'};
    } catch (e) {
      return {'success': false, 'error': '$e'};
    }
  }

  Future<void> logout() async {
    _token = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
  }
}
