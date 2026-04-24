import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/api_service.dart';

enum AuthStatus { authenticated, unauthenticated, guest, authenticating }

class AuthProvider with ChangeNotifier {
  AuthStatus _status = AuthStatus.unauthenticated;
  Map<String, dynamic>? _user;
  String? _token;
  String? _errorMessage;
  final ApiService _apiService = ApiService();

  AuthStatus get status => _status;
  Map<String, dynamic>? get user => _user;
  String? get token => _token;
  String? get errorMessage => _errorMessage;
  bool get isGuest => _status == AuthStatus.guest;
  bool get isLoading => _status == AuthStatus.authenticating;
  bool get isAdmin {
    final role = _user?['role']?.toString().toLowerCase();
    final tier = _user?['tier']?.toString().toUpperCase();
    return role == 'admin' || tier == 'ADMIN';
  }

  AuthProvider() {
    _checkAuth();
  }

  Future<void> _checkAuth() async {
    _status = AuthStatus.authenticating;
    notifyListeners();

    try {
      final prefs = await SharedPreferences.getInstance();
      _token = prefs.getString('auth_token');
      final isGuestMode = prefs.getBool('is_guest') ?? false;

      if (_token != null) {
        // Fetch user profile from backend
        final result = await _apiService.getUserProfile();
        if (result['success'] == true) {
          _user = result['data'] as Map<String, dynamic>?;
          _status = AuthStatus.authenticated;
        } else {
          // Token invalid, clear
          await prefs.clear();
          _token = null;
          _status = AuthStatus.unauthenticated;
        }
      } else if (isGuestMode) {
        _status = AuthStatus.guest;
      } else {
        _status = AuthStatus.unauthenticated;
      }
    } catch (e) {
      _errorMessage = 'Failed to check authentication status.';
      _status = AuthStatus.unauthenticated;
    }
    notifyListeners();
  }

  Future<bool> login(String email, String password) async {
    _status = AuthStatus.authenticating;
    _errorMessage = null;
    notifyListeners();

    try {
      // Register/login with backend
      final result =
          await _apiService.register(email, password, email.split('@')[0]);

      if (result['success'] == true) {
        final data = result['data'] as Map<String, dynamic>;
        _token = data['token'] ?? data['idToken'];
        _user = data['user'] as Map<String, dynamic>?;
        _status = AuthStatus.authenticated;
        notifyListeners();

        final prefs = await SharedPreferences.getInstance();
        if (_token != null) {
          await prefs.setString('auth_token', _token!);
        }
        return true;
      } else {
        _errorMessage =
            result['error'] ?? 'Login failed. Please check your credentials.';
        _status = AuthStatus.unauthenticated;
        notifyListeners();
        return false;
      }
    } catch (e) {
      _errorMessage = 'Login failed. Please check your credentials.';
      _status = AuthStatus.unauthenticated;
      notifyListeners();
      return false;
    }
  }

  Future<void> continueAsGuest() async {
    _status = AuthStatus.authenticating;
    notifyListeners();

    try {
      _status = AuthStatus.guest;
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('is_guest', true);
      notifyListeners();
    } catch (e) {
      _errorMessage = 'Failed to continue as guest.';
      _status = AuthStatus.unauthenticated;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    _status = AuthStatus.authenticating;
    notifyListeners();

    try {
      await _apiService.logout();
      _status = AuthStatus.unauthenticated;
      _token = null;
      _user = null;
      _errorMessage = null;
      notifyListeners();
    } catch (e) {
      _errorMessage = 'Failed to logout.';
      _status = AuthStatus.unauthenticated;
      notifyListeners();
    }
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }
}
