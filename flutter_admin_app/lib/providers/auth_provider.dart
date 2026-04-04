import 'package:flutter/material.dart';
import '../services/auth_service.dart';

class AuthProvider extends ChangeNotifier {
  final AuthService _authService = AuthService();

  bool _isLoading = false;
  bool _isLoggedIn = false;
  String? _error;
  Map<String, dynamic>? _currentUser;

  // Getters
  bool get isLoading => _isLoading;
  bool get isLoggedIn => _isLoggedIn;
  String? get error => _error;
  Map<String, dynamic>? get currentUser => _currentUser;

  // Check login status
  Future<bool> checkLoginStatus() async {
    _isLoggedIn = false;
    _currentUser = null;

    final hasStoredSession = await _authService.isLoggedIn();
    if (hasStoredSession) {
      var isTokenValid = await _authService.isTokenValid();

      if (!isTokenValid) {
        final refreshed = await _authService.refreshToken();
        if (refreshed) {
          isTokenValid = await _authService.isTokenValid();
        }
      }

      if (isTokenValid) {
        _isLoggedIn = true;
        _currentUser = await _authService.getCurrentUser();
      } else {
        await _authService.logout();
      }
    }
    
    notifyListeners();
    return _isLoggedIn;
  }

  // Login
  Future<bool> login(String email, String password) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final success = await _authService.login(email, password);
      
      if (success) {
        _isLoggedIn = true;
        _currentUser = await _authService.getCurrentUser();
        _isLoading = false;
        notifyListeners();
        return true;
      } else {
        _error = _authService.lastError ?? 'Invalid email or password';
        _isLoading = false;
        notifyListeners();
        return false;
      }
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  // Register
  Future<bool> register(String email, String password, String name) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final success = await _authService.register(email, password, name);
      
      if (success) {
        _isLoggedIn = true;
        _currentUser = await _authService.getCurrentUser();
        _isLoading = false;
        notifyListeners();
        return true;
      } else {
        _error = _authService.lastError ?? 'Registration failed. Please try again.';
        _isLoading = false;
        notifyListeners();
        return false;
      }
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  // Logout
  Future<void> logout() async {
    _isLoading = true;
    notifyListeners();

    try {
      await _authService.logout();
      _isLoggedIn = false;
      _currentUser = null;
      _error = null;
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  // Clear error
  void clearError() {
    _error = null;
    notifyListeners();
  }
}
