import 'dart:convert';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:jwt_decoder/jwt_decoder.dart';
import '../config/environment.dart';
import 'api_service.dart';
import 'storage_service.dart';

class AuthService {
  static final AuthService _instance = AuthService._internal();
  final ApiService _apiService = ApiService();
  final StorageService _storageService = StorageService();
  String? _lastError;

  factory AuthService() {
    return _instance;
  }

  AuthService._internal();

  String? get lastError => _lastError;

  // Login using Firebase Auth email/password + backend token exchange
  Future<bool> login(String email, String password) async {
    _lastError = null;
    try {
      final cred = await FirebaseAuth.instance.signInWithEmailAndPassword(
        email: email,
        password: password,
      );
      final idToken = await cred.user?.getIdToken();
      if (idToken == null) {
        _lastError = 'Firebase ID token not available';
        return false;
      }

      final response = await _apiService.post<Map<String, dynamic>>(
        '/api/auth/firebase-login',
        data: {'idToken': idToken},
      );

      if (response.success && response.data != null) {
        final token = response.data?['token'] as String?;
        final refreshToken = response.data?['refreshToken'] as String?;

        if (token != null) {
          await _storageService.saveToken(token);
          if (refreshToken != null) {
            await _storageService.saveRefreshToken(refreshToken);
          }

          // Extract and save user data
          final decodedToken = JwtDecoder.decode(token);
          await _storageService.saveUserData(jsonEncode(decodedToken));

          return true;
        }
      }

      _lastError = response.error ?? 'Login failed';
      return false;
    } catch (e) {
      print('Login error: $e');
      _lastError = 'Firebase login failed: $e';
      return false;
    }
  }

  // Register (for admin only)
  Future<bool> register(String email, String password, String name) async {
    _lastError = null;
    try {
      final username = _buildUsername(name, email);
      final response = await _apiService.post<Map<String, dynamic>>(
        Environment.authRegister,
        data: {
          'username': username,
          'email': email,
          'password': password,
          'name': name,
        },
      );

      if (response.success && response.data != null) {
        final token = response.data?['token'] as String?;
        final refreshToken = response.data?['refreshToken'] as String?;

        if (token != null) {
          await _storageService.saveToken(token);
          if (refreshToken != null) {
            await _storageService.saveRefreshToken(refreshToken);
          }

          // Extract and save user data
          final decodedToken = JwtDecoder.decode(token);
          await _storageService.saveUserData(jsonEncode(decodedToken));

          return true;
        }
      }

      _lastError = response.error ?? 'Registration failed';
      return false;
    } catch (e) {
      print('Register error: $e');
      _lastError = e.toString();
      return false;
    }
  }

  String _buildUsername(String name, String email) {
    final normalizedName = name.trim().toLowerCase().replaceAll(RegExp(r'[^a-z0-9]+'), '.');
    final collapsedName = normalizedName.replaceAll(RegExp(r'^\.+|\.+$'), '');
    if (collapsedName.isNotEmpty) {
      return collapsedName;
    }

    final emailPrefix = email.split('@').first.trim().toLowerCase().replaceAll(RegExp(r'[^a-z0-9._-]+'), '');
    return emailPrefix.isNotEmpty ? emailPrefix : 'admin.user';
  }

  // Logout
  Future<void> logout() async {
    try {
      await _apiService.post(Environment.authLogout);
    } catch (e) {
      print('Logout error: $e');
    }
    // Sign out of Firebase Auth as well
    try {
      await FirebaseAuth.instance.signOut();
    } catch (e) {
      print('Firebase sign-out error: $e');
    }
    await _storageService.clearToken();
    await _storageService.clearUserData();
  }

  // Refresh token
  Future<bool> refreshToken() async {
    try {
      final refreshToken = await _storageService.getRefreshToken();
      if (refreshToken == null) return false;

      final response = await _apiService.post<Map<String, dynamic>>(
        Environment.authRefresh,
        data: {'refreshToken': refreshToken},
      );

      if (response.success && response.data != null) {
        final newToken = response.data?['token'] as String?;
        if (newToken != null) {
          await _storageService.saveToken(newToken);
          return true;
        }
      }

      return false;
    } catch (e) {
      print('Token refresh error: $e');
      return false;
    }
  }

  // Check if token is valid
  Future<bool> isTokenValid() async {
    final token = await _storageService.getToken();
    if (token == null) return false;

    try {
      return !JwtDecoder.isExpired(token);
    } catch (e) {
      return false;
    }
  }

  // Get current user info
  Future<Map<String, dynamic>?> getCurrentUser() async {
    final userData = await _storageService.getUserData();
    if (userData != null) {
      return jsonDecode(userData);
    }
    return null;
  }

  // Get token
  Future<String?> getToken() async {
    return await _storageService.getToken();
  }

  // Check if user is logged in
  Future<bool> isLoggedIn() async {
    return await _storageService.isLoggedIn();
  }
}
