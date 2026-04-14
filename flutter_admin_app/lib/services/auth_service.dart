import 'package:firebase_auth/firebase_auth.dart';
import 'api_service.dart';
import 'storage_service.dart';

class AuthService {
  static final AuthService _instance = AuthService._internal();
  String? _lastError;

  factory AuthService() {
    return _instance;
  }

  AuthService._internal();

  String? get lastError => _lastError;

  Future<bool> login(String email, String password) async {
    _lastError = null;
    try {
      final UserCredential credential =
          await FirebaseAuth.instance.signInWithEmailAndPassword(
        email: email,
        password: password,
      );

      // 1. Get Firebase ID Token
      final String? idToken = await credential.user?.getIdToken();
      if (idToken == null) throw Exception('Failed to get Firebase ID token');

      // 2. Exchange for Backend JWT
      final response = await ApiService().post<Map<String, dynamic>>(
        '/api/auth/firebase-login',
        data: {'idToken': idToken},
      );

      if (response.success && response.data != null) {
        final String backendToken = response.data!['token'];
        // Store backend token for ApiService
        await StorageService().saveToken(backendToken);
        return true;
      } else {
        _lastError = response.error ?? 'Backend authentication failed';
        await FirebaseAuth.instance.signOut();
        return false;
      }
    } on FirebaseAuthException catch (e) {
      _lastError = e.message ?? 'Firebase login failed';
      return false;
    } catch (e) {
      _lastError = 'Firebase login failed: $e';
      return false;
    }
  }

  Future<bool> register(String email, String password, String name) async {
    _lastError = null;
    try {
      final credential =
          await FirebaseAuth.instance.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );

      await credential.user?.updateDisplayName(name.trim());
      await credential.user?.reload();

      // After registration, the user is signed in to Firebase.
      // We should also get a backend token for them.
      final String? idToken = await credential.user?.getIdToken();
      if (idToken != null) {
        final response = await ApiService().post<Map<String, dynamic>>(
          '/api/auth/firebase-login',
          data: {'idToken': idToken},
        );
        if (response.success && response.data != null) {
          await StorageService().saveToken(response.data!['token']);
        }
      }

      return true;
    } on FirebaseAuthException catch (e) {
      _lastError = e.message ?? 'Registration failed';
      return false;
    } catch (e) {
      _lastError = e.toString();
      return false;
    }
  }

  Future<void> logout() async {
    try {
      await FirebaseAuth.instance.signOut();
      await StorageService().deleteToken();
    } catch (_) {
      // Ignore sign-out cleanup failures.
    }
  }

  Future<bool> refreshToken() async {
    try {
      final user = FirebaseAuth.instance.currentUser;
      if (user == null) {
        return false;
      }

      await user.getIdToken(true);
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<bool> isTokenValid() async {
    try {
      final user = FirebaseAuth.instance.currentUser;
      if (user == null) {
        return false;
      }

      await user.getIdTokenResult();
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<Map<String, dynamic>?> getCurrentUser() async {
    final user = FirebaseAuth.instance.currentUser;
    if (user == null) {
      return null;
    }

    return <String, dynamic>{
      'uid': user.uid,
      'email': user.email,
      'displayName': user.displayName,
      'isAnonymous': user.isAnonymous,
      'emailVerified': user.emailVerified,
    };
  }

  Future<String?> getToken() async {
    return await StorageService().getToken();
  }

  Future<bool> isLoggedIn() async {
    try {
      return FirebaseAuth.instance.currentUser != null;
    } on FirebaseException {
      // Tests can render widgets without initializing Firebase.
      return false;
    } catch (_) {
      return false;
    }
  }
}
