import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:supremeai/providers/auth_provider.dart';
import 'package:supremeai/services/api_service.dart';
import 'package:http/http.dart' as http;

class FakeApiService implements ApiService {
  @override
  http.Client get client => throw UnimplementedError();

  bool registerSuccess = true;
  Map<String, dynamic> registerData = {};
  String? registerError;

  @override
  Future<Map<String, dynamic>> register(String email, String password, String displayName) async {
    if (registerSuccess) {
      return {'success': true, 'data': registerData};
    } else {
      return {'success': false, 'error': registerError ?? 'Error'};
    }
  }

  @override
  Future<Map<String, dynamic>> getUserProfile() async => {'success': false};

  @override
  Future<Map<String, dynamic>> firebaseLogin(String idToken) async => {'success': false};

  @override
  Future<void> logout() async {}

  @override
  Future<List<Map<String, dynamic>>> getConfiguredProviders() async => [];

  @override
  Future<String?> getToken() async => null;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('AuthProvider Edge Cases', () {
    setUp(() {
      SharedPreferences.setMockInitialValues({});
    });

    test('login failure sets unauthenticated and error', () async {
      final fakeApi = FakeApiService();
      fakeApi.registerSuccess = false;
      fakeApi.registerError = 'Invalid credentials';

      final auth = AuthProvider(apiService: fakeApi);
      await Future(() {});
      await Future(() {});

      final result = await auth.login('bad@test.com', 'wrong');
      expect(result, false);
      expect(auth.status, AuthStatus.unauthenticated);
      expect(auth.errorMessage, 'Invalid credentials');
    });

    test('loginWithGoogle returns false when sign-in cancelled', () async {
      final fakeApi = FakeApiService();
      final auth = AuthProvider(apiService: fakeApi);
      await Future(() {});
      await Future(() {});

      expect(auth.status, AuthStatus.guest);
    });

    test('logout clears auth state', () async {
      SharedPreferences.setMockInitialValues({'auth_token': 'tok', 'user_json': '{"username":"admin"}'});
      final fakeApi = FakeApiService();
      final auth = AuthProvider(apiService: fakeApi);
      await Future(() {});
      await Future(() {});

      await auth.logout();
      await Future(() {});
      expect(auth.token, null);
      expect(auth.user, null);
      expect(auth.status, AuthStatus.unauthenticated);
    });
  });
}
