import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum AuthStatus { authenticated, unauthenticated, guest, authenticating }

class AuthProvider with ChangeNotifier {
  AuthStatus _status = AuthStatus.unauthenticated;
  Map<String, dynamic>? _user;
  String? _token;

  AuthStatus get status => _status;
  Map<String, dynamic>? get user => _user;
  bool get isGuest => _status == AuthStatus.guest;

  AuthProvider() {
    _checkAuth();
  }

  Future<void> _checkAuth() async {
    final prefs = await SharedPreferences.getInstance();
    _token = prefs.getString('auth_token');
    final isGuestMode = prefs.getBool('is_guest') ?? false;

    if (_token != null) {
      _status = AuthStatus.authenticated;
      // Fetch user profile logic here
    } else if (isGuestMode) {
      _status = AuthStatus.guest;
    } else {
      _status = AuthStatus.unauthenticated;
    }
    notifyListeners();
  }

  void continueAsGuest() async {
    _status = AuthStatus.guest;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('is_guest', true);
    notifyListeners();
  }

  void logout() async {
    _status = AuthStatus.unauthenticated;
    _token = null;
    _user = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    notifyListeners();
  }
}
