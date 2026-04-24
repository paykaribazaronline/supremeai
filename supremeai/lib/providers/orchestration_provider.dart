import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'dart:async';
import 'package:connectivity_plus/connectivity_plus.dart';

enum OrchestrationErrorType {
  network,
  serverError,
  unauthorized,
  timeout,
  unknown,
}

class OrchestrationError {
  final String message;
  final OrchestrationErrorType type;
  final bool recoverable;

  OrchestrationError({
    required this.message,
    required this.type,
    this.recoverable = true,
  });

  factory OrchestrationError.fromException(dynamic e) {
    if (e is TimeoutException) {
      return OrchestrationError(
        message:
            'The request timed out. Please check your connection and try again.',
        type: OrchestrationErrorType.timeout,
      );
    }
    if (e is http.ClientException) {
      return OrchestrationError(
        message:
            'Unable to connect to the server. Please check your internet connection.',
        type: OrchestrationErrorType.network,
      );
    }
    return OrchestrationError(
      message: 'An unexpected error occurred: ${e.toString()}',
      type: OrchestrationErrorType.unknown,
    );
  }

  factory OrchestrationError.fromStatusCode(int code) {
    switch (code) {
      case 401:
        return OrchestrationError(
          message: 'Your session has expired. Please log in again.',
          type: OrchestrationErrorType.unauthorized,
          recoverable: false,
        );
      case 403:
        return OrchestrationError(
          message: 'You do not have permission to perform this action.',
          type: OrchestrationErrorType.unauthorized,
          recoverable: false,
        );
      case 503:
        return OrchestrationError(
          message:
              'The service is temporarily unavailable. Please try again later.',
          type: OrchestrationErrorType.serverError,
        );
      default:
        return OrchestrationError(
          message: 'Server error (HTTP $code). Please try again.',
          type: OrchestrationErrorType.serverError,
        );
    }
  }
}

class OrchestrationProvider with ChangeNotifier {
  bool _isLoading = false;
  Map<String, dynamic>? _lastResult;
  OrchestrationError? _error;
  bool _isOnline = true;
  List<Map<String, dynamic>> _offlineQueue = [];

  bool get isLoading => _isLoading;
  Map<String, dynamic>? get lastResult => _lastResult;
  OrchestrationError? get error => _error;
  bool get isOnline => _isOnline;
  List<Map<String, dynamic>> get offlineQueue => _offlineQueue;

  static const String _baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080',
  );

  static const String _cachedResultKey = 'orchestration_cached_result';
  static const String _offlineQueueKey = 'orchestration_offline_queue';

  OrchestrationProvider() {
    _initConnectivity();
    _loadCachedData();
  }

  Future<void> _initConnectivity() async {
    try {
      final result = await Connectivity().checkConnectivity();
      _updateConnectionStatus(result);

      Connectivity()
          .onConnectivityChanged
          .listen((List<ConnectivityResult> result) {
        _updateConnectionStatus(result);
      });
    } catch (_) {
      // Ignore connectivity check errors
    }
  }

  void _updateConnectionStatus(List<ConnectivityResult> result) {
    final wasOnline = _isOnline;
    _isOnline = result.isNotEmpty && result.first != ConnectivityResult.none;
    if (wasOnline != _isOnline) {
      notifyListeners();
    }
  }

  Future<void> _loadCachedData() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final cachedResult = prefs.getString(_cachedResultKey);
      if (cachedResult != null) {
        _lastResult = json.decode(cachedResult);
      }
      final cachedQueue = prefs.getString(_offlineQueueKey);
      if (cachedQueue != null) {
        _offlineQueue =
            List<Map<String, dynamic>>.from(json.decode(cachedQueue));
      }
      notifyListeners();
    } catch (_) {
      // Ignore cache loading errors
    }
  }

  Future<void> _cacheResult(Map<String, dynamic> result) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_cachedResultKey, json.encode(result));
    } catch (_) {
      // Ignore caching errors
    }
  }

  Future<void> _queueOfflineOperation(Map<String, dynamic> operation) async {
    _offlineQueue.add(operation);
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_offlineQueueKey, json.encode(_offlineQueue));
    } catch (_) {
      // Ignore queue saving errors
    }
  }

  void _clearError() {
    _error = null;
  }

  Future<void> orchestrateRequirement(String requirement, String token) async {
    _isLoading = true;
    _clearError();
    notifyListeners();

    // If offline, queue the operation
    if (!_isOnline) {
      await _queueOfflineOperation({
        'type': 'orchestrate',
        'requirement': requirement,
        'timestamp': DateTime.now().toIso8601String(),
      });
      _error = OrchestrationError(
        message:
            'You are currently offline. Your request has been queued and will be processed when connection is restored.',
        type: OrchestrationErrorType.network,
      );
      _isLoading = false;
      notifyListeners();
      return;
    }

    try {
      final response = await http
          .post(
            Uri.parse('$_baseUrl/api/orchestrate/requirement'),
            headers: {
              'Content-Type': 'application/json',
              'Authorization': 'Bearer $token',
            },
            body: json.encode({'requirement': requirement}),
          )
          .timeout(
            const Duration(seconds: 30),
            onTimeout: () => throw TimeoutException('Request timed out'),
          );

      if (response.statusCode == 200) {
        _lastResult = json.decode(response.body);
        await _cacheResult(_lastResult!);
      } else {
        _error = OrchestrationError.fromStatusCode(response.statusCode);
      }
    } catch (e) {
      _error = OrchestrationError.fromException(e);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> generateProject(String token) async {
    if (_lastResult == null) {
      _error = OrchestrationError(
        message: 'No orchestration result available to generate a project.',
        type: OrchestrationErrorType.unknown,
        recoverable: false,
      );
      notifyListeners();
      return;
    }

    _isLoading = true;
    _clearError();
    notifyListeners();

    try {
      final response = await http
          .post(
            Uri.parse('$_baseUrl/api/orchestrate/generate'),
            headers: {
              'Content-Type': 'application/json',
              'Authorization': 'Bearer $token',
            },
            body: json.encode(_lastResult),
          )
          .timeout(
            const Duration(seconds: 60),
            onTimeout: () => throw TimeoutException('Request timed out'),
          );

      if (response.statusCode == 200) {
        _lastResult = json.decode(response.body);
      } else {
        _error = OrchestrationError.fromStatusCode(response.statusCode);
      }
    } catch (e) {
      _error = OrchestrationError.fromException(e);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
