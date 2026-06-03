import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:supremeai/providers/auth_provider.dart';
import 'package:supremeai/screens/dashboard/home_screen.dart';
import 'package:supremeai/services/localization_service.dart';

class FakeOrchestrationProvider extends ChangeNotifier {
  bool _isLoading = false;
  String? _errorMessage;

  bool get isLoading => _isLoading;
  String? get error => _errorMessage;

  void setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  void setError(String message) {
    _errorMessage = message;
    notifyListeners();
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  Future<void> orchestrateRequirement(String requirement, String token, {String? geminiKey}) async {
    _isLoading = true;
    notifyListeners();
    await Future<void>.delayed(const Duration(milliseconds: 10));
    _isLoading = false;
    notifyListeners();
  }

  Future<void> generateProject(String token) async {}
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    LocalizationService.setMockData({
      'app': {'title': 'SupremeAI'},
      'chat': {'describe': 'Describe what you want to build...'},
      'nav': {'chat': 'Chat', 'skills': 'Skills'},
    });
  });

  Widget createHomeScreen({AuthProvider? authProvider, FakeOrchestrationProvider? orchestration}) {
    final auth = authProvider ?? AuthProvider();
    final orch = orchestration ?? FakeOrchestrationProvider();

    return MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthProvider>.value(value: auth),
        ChangeNotifierProvider<FakeOrchestrationProvider>.value(value: orch),
      ],
      child: const MaterialApp(home: HomeScreen()),
    );
  }

  testWidgets('HomeScreen renders title and chat input', (WidgetTester tester) async {
    await tester.pumpWidget(createHomeScreen());
    await tester.pumpAndSettle();

    expect(find.text('SupremeAI'), findsOneWidget);
    expect(find.byType(TextField), findsOneWidget);
  });

  testWidgets('HomeScreen shows empty state prompt when no messages', (WidgetTester tester) async {
    await tester.pumpWidget(createHomeScreen());
    await tester.pumpAndSettle();

    expect(find.text('Describe what you want to build...'), findsOneWidget);
  });

  testWidgets('HomeScreen bottom navigation bar renders 5 destinations', (WidgetTester tester) async {
    await tester.pumpWidget(createHomeScreen());
    await tester.pumpAndSettle();

    expect(find.byType(NavigationDestination), findsNWidgets(5));
  });

  testWidgets('typing in chat input updates field value', (WidgetTester tester) async {
    await tester.pumpWidget(createHomeScreen());
    await tester.pumpAndSettle();

    final input = find.byType(TextField).first;
    await tester.enterText(input, 'Build a todo app');
    await tester.pump();

    expect(find.text('Build a todo app'), findsOneWidget);
  });

  testWidgets('shows loading indicator when orchestration is loading', (WidgetTester tester) async {
    final orch = FakeOrchestrationProvider();
    orch.setLoading(true);

    await tester.pumpWidget(createHomeScreen(orchestration: orch));
    await tester.pumpAndSettle();

    expect(find.byType(LinearProgressIndicator), findsOneWidget);
  });

  testWidgets('displays error banner when orchestration has error', (WidgetTester tester) async {
    final orch = FakeOrchestrationProvider();
    orch.setError('Network down');

    await tester.pumpWidget(createHomeScreen(orchestration: orch));
    await tester.pumpAndSettle();

    expect(find.text('Network down'), findsOneWidget);
  });

  testWidgets('logout button present in app bar when authenticated', (WidgetTester tester) async {
    final auth = AuthProvider();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('auth_token', 'token-abc');
    await prefs.setString('user_json', '{"username":"admin","role":"admin"}');

    await tester.pumpWidget(createHomeScreen(authProvider: auth));
    await tester.pumpAndSettle(const Duration(seconds: 1));

    expect(find.byIcon(Icons.logout), findsOneWidget);
  });

  testWidgets('login icon shown when in guest mode', (WidgetTester tester) async {
    final auth = AuthProvider();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('is_guest', true);

    await tester.pumpWidget(createHomeScreen(authProvider: auth));
    await tester.pumpAndSettle(const Duration(seconds: 1));

    expect(find.byIcon(Icons.login), findsOneWidget);
  });
}
