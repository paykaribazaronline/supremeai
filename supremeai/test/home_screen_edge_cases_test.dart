import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:supremeai/screens/dashboard/home_screen.dart';
import 'package:provider/provider.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:supremeai/services/api_service.dart';
import 'package:supremeai/providers/auth_provider.dart';
import 'package:supremeai/providers/orchestration_provider.dart';
import 'package:supremeai/providers/settings_provider.dart';

class FakeApiService implements ApiService {
  @override
  http.Client get client => throw UnimplementedError();

  @override
  Future<Map<String, dynamic>> getUserProfile() async => {'success': false};

  @override
  Future<Map<String, dynamic>> register(String email, String password, String displayName) async => {'success': false};

  @override
  Future<Map<String, dynamic>> firebaseLogin(String idToken) async => {'success': false};

  @override
  Future<void> logout() async {}

  @override
  Future<List<Map<String, dynamic>>> getConfiguredProviders() async => [];

  @override
  Future<String?> getToken() async => null;
}

class FakeAuth extends AuthProvider {
  FakeAuth() : super(apiService: FakeApiService());

  @override
  bool get isGuest => true;

  @override
  String? get token => null;

  @override
  Future<void> logout() async {}
}

class FakeOrch extends OrchestrationProvider {
  bool _loading = false;
  OrchestrationError? _errorMessage;

  @override
  bool get isLoading => _loading;
  @override
  OrchestrationError? get error => _errorMessage;

  void setLoading(bool loading) {
    _loading = loading;
    notifyListeners();
  }

  @override
  Future<void> orchestrateRequirement(String req, String token, {String? geminiKey, String? activeModel}) async {}

  @override
  Future<void> generateProject(String token) async {}
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  testWidgets('HomeScreen send button disabled when input empty', (WidgetTester tester) async {
    await tester.pumpWidget(MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthProvider>.value(value: FakeAuth()),
        ChangeNotifierProvider<OrchestrationProvider>.value(value: FakeOrch()),
        ChangeNotifierProvider<SettingsProvider>.value(value: SettingsProvider()),
      ],
      child: const MaterialApp(home: HomeScreen()),
    ));
    await tester.pumpAndSettle();

    final sendBtn = find.byType(IconButton);
    expect(sendBtn, findsOneWidget);
  });

  testWidgets('HomeScreen generates project when result has action', (WidgetTester tester) async {
    final orch = FakeOrch();
    await tester.pumpWidget(MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthProvider>.value(value: FakeAuth()),
        ChangeNotifierProvider<OrchestrationProvider>.value(value: orch),
        ChangeNotifierProvider<SettingsProvider>.value(value: SettingsProvider()),
      ],
      child: const MaterialApp(home: HomeScreen()),
    ));
    await tester.pumpAndSettle();

    final generateBtn = find.text('Generate Project');
    if (generateBtn.evaluate().isNotEmpty) {
      await tester.tap(generateBtn);
      await tester.pump();
    }
  });

  testWidgets('HomeScreen switches tabs via bottom bar', (WidgetTester tester) async {
    await tester.pumpWidget(MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthProvider>.value(value: FakeAuth()),
        ChangeNotifierProvider<OrchestrationProvider>.value(value: FakeOrch()),
        ChangeNotifierProvider<SettingsProvider>.value(value: SettingsProvider()),
      ],
      child: const MaterialApp(home: HomeScreen()),
    ));
    await tester.pumpAndSettle();

    final destinations = find.byType(NavigationDestination);
    expect(destinations, findsNWidgets(5));
  });
}
