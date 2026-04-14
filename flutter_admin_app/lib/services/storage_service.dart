import 'package:shared_preferences/shared_preferences.dart';

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

  // Token Methods
  static const String _tokenKey = 'auth_token';

  Future<void> saveToken(String token) async {
    await setString(_tokenKey, token);
  }

  Future<String?> getToken() async {
    return await getString(_tokenKey);
  }

  Future<void> deleteToken() async {
    await remove(_tokenKey);
  }
}
