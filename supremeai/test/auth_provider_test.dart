import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:supremeai/providers/auth_provider.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('AuthProvider Tests', () {
    setUp(() {
      SharedPreferences.setMockInitialValues({});
    });

    test('Initial status should be unauthenticated', () async {
      final authProvider = AuthProvider();
      // Since it's an async constructor call via _checkAuth, 
      // we need to wait a bit or use a microtask
      await Future.delayed(Duration.zero);
      expect(authProvider.status, AuthStatus.unauthenticated);
    });

    test('continueAsGuest should update status to guest', () async {
      final authProvider = AuthProvider();
      authProvider.continueAsGuest();
      
      expect(authProvider.status, AuthStatus.guest);
      expect(authProvider.isGuest, true);
      
      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getBool('is_guest'), true);
    });

    test('logout should clear status and token', () async {
      SharedPreferences.setMockInitialValues({
        'auth_token': 'test_token',
        'is_guest': true,
      });
      
      final authProvider = AuthProvider();
      await Future.delayed(Duration.zero);
      
      authProvider.logout();
      
      expect(authProvider.status, AuthStatus.unauthenticated);
      expect(authProvider.user, null);
      
      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getString('auth_token'), null);
    });
  });
}
