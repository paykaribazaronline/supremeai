import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class OrchestrationProvider with ChangeNotifier {
  bool _isLoading = false;
  Map<String, dynamic>? _lastResult;
  String? _error;

  bool get isLoading => _isLoading;
  Map<String, dynamic>? get lastResult => _lastResult;
  String? get error => _error;

  Future<void> orchestrateRequirement(String requirement, String token) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await http.post(
        Uri.parse('https://supremeai-lhlwyikwlq-uc.a.run.app/api/orchestrate/requirement'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: json.encode({'requirement': requirement}),
      );

      if (response.statusCode == 200) {
        _lastResult = json.decode(response.body);
      } else {
        _error = 'Failed: ${response.statusCode}';
        Future<void> generateProject(String token) async {
    if (_lastResult == null) return;

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await http.post(
        Uri.parse('https://supremeai-lhlwyikwlq-uc.a.run.app/api/orchestrate/generate'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: json.encode(_lastResult),
      );

      if (response.statusCode == 200) {
        _lastResult = json.decode(response.body);
      } else {
        _error = 'Generation Failed: ${response.statusCode}';
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
      Future<void> generateProject(String token) async {
    if (_lastResult == null) return;

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await http.post(
        Uri.parse('https://supremeai-lhlwyikwlq-uc.a.run.app/api/orchestrate/generate'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: json.encode(_lastResult),
      );

      if (response.statusCode == 200) {
        _lastResult = json.decode(response.body);
      } else {
        _error = 'Generation Failed: ${response.statusCode}';
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
    Future<void> generateProject(String token) async {
    if (_lastResult == null) return;

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await http.post(
        Uri.parse('https://supremeai-lhlwyikwlq-uc.a.run.app/api/orchestrate/generate'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: json.encode(_lastResult),
      );

      if (response.statusCode == 200) {
        _lastResult = json.decode(response.body);
      } else {
        _error = 'Generation Failed: ${response.statusCode}';
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
  Future<void> generateProject(String token) async {
    if (_lastResult == null) return;

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await http.post(
        Uri.parse('https://supremeai-lhlwyikwlq-uc.a.run.app/api/orchestrate/generate'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: json.encode(_lastResult),
      );

      if (response.statusCode == 200) {
        _lastResult = json.decode(response.body);
      } else {
        _error = 'Generation Failed: ${response.statusCode}';
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
