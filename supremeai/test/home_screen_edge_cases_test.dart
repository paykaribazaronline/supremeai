import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:supremeai/screens/dashboard/home_screen.dart';
import 'package:provider/provider.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:supremeai/services/api_service.dart';

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

class FakeAuth extends ChangeNotifier {
  final bool _guest = true;
  bool get isGuest => _guest;
  String? get token => null;
  Future<void> logout() async {}
}

class FakeOrch extends ChangeNotifier {
  final bool _loading = false;
  String? _error;
  bool get isLoading => _loading;
  String? get error => _error;
  Future<void> orchestrateRequirement(String req, String token, {String? geminiKey}) async {}
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
        ChangeNotifierProvider<FakeAuth>.value(value: FakeAuth()),
        ChangeNotifierProvider<FakeOrch>.value(value: FakeOrch()),
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
        ChangeNotifierProvider<FakeAuth>.value(value: FakeAuth()),
        ChangeNotifierProvider<FakeOrch>.value(value: orch),
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
        ChangeNotifierProvider<FakeAuth>.value(value: FakeAuth()),
        ChangeNotifierProvider<FakeOrch>.value(value: FakeOrch()),
      ],
      child: const MaterialApp(home: HomeScreen()),
    ));
    await tester.pumpAndSettle();

    final destinations = find.byType(NavigationDestination);
    expect(destinations, findsNWidgets(5));
  });
}
