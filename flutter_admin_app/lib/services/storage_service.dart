import 'package:shared_preferences/shared_preferences.dart';
import '../config/environment.dart';

class StorageService {
  static final StorageService _instance = StorageService._internal();
  late SharedPreferences _prefs;
  bool _initialized = false;

  factory StorageService() {
    return _instance;
  }

  StorageService._internal();

  Future<void> init() async {
    if (!_initialized) {
      _prefs = await SharedPreferences.getInstance();
      _initialized = true;
    }
  }

  // Token Management
  Future<void> saveToken(String token) async {
    await init();
    await _prefs.setString(Environment.tokenStorageKey, token);
  }

  Future<String?> getToken() async {
    await init();
    return _prefs.getString(Environment.tokenStorageKey);
  }

  Future<void> saveRefreshToken(String token) async {
    await init();
    await _prefs.setString(Environment.refreshTokenStorageKey, token);
  }

  Future<String?> getRefreshToken() async {
    await init();
    return _prefs.getString(Environment.refreshTokenStorageKey);
  }

  Future<void> clearToken() async {
    await init();
    await _prefs.remove(Environment.tokenStorageKey);
    await _prefs.remove(Environment.refreshTokenStorageKey);
  }

  // User Data
  Future<void> saveUserData(String userData) async {
    await init();
    await _prefs.setString(Environment.userStorageKey, userData);
  }

  Future<String?> getUserData() async {
    await init();
    return _prefs.getString(Environment.userStorageKey);
  }

  Future<void> clearUserData() async {
    await init();
    await _prefs.remove(Environment.userStorageKey);
  }

  // Generic Methods
  Future<void> setString(String key, String value) async {
    await init();
    await _prefs.setString(key, value);
  }

  Future<String?> getString(String key) async {
    await init();
    return _prefs.getString(key);
  }

  Future<void> setInt(String key, int value) async {
    await init();
    await _prefs.setInt(key, value);
  }

  Future<int?> getInt(String key) async {
    await init();
    return _prefs.getInt(key);
  }

  Future<void> setBool(String key, bool value) async {
    await init();
    await _prefs.setBool(key, value);
  }

  Future<bool?> getBool(String key) async {
    await init();
    return _prefs.getBool(key);
  }

  Future<void> remove(String key) async {
    await init();
    await _prefs.remove(key);
  }

  Future<void> clearAll() async {
    await init();
    await _prefs.clear();
  }

  // Check if user is logged in
  Future<bool> isLoggedIn() async {
    final token = await getToken();
    return token != null && token.isNotEmpty;
  }
}
