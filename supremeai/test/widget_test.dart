import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:supremeai/main.dart';
import 'package:supremeai/providers/auth_provider.dart';
import 'package:supremeai/providers/settings_provider.dart';
import 'package:supremeai/providers/orchestration_provider.dart';
import 'package:supremeai/screens/login_screen.dart';
import 'package:supremeai/screens/dashboard/home_screen.dart';
import 'package:supremeai/services/localization_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    LocalizationService.setMockData({
      'app': {'title': 'SupremeAI'},
      'btn': {'login': 'Login'},
      'nav': {'dashboard': 'Dashboard'},
      'onboarding': {'rate_limiting_desc': 'Test Description'}
    });
  });

  Widget createTestApp() {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => SettingsProvider()),
        ChangeNotifierProvider(create: (_) => OrchestrationProvider()),
      ],
      child: const MyApp(),
    );
  }

  testWidgets('Initial screen is LoginScreen when not authenticated',
      (WidgetTester tester) async {
    await tester.pumpWidget(createTestApp());

    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.text('app.title'.tr()), findsOneWidget);
    expect(find.text('btn.login'.tr()), findsOneWidget);
  });

  testWidgets('Clicking Guest Mode navigates to HomeScreen',
      (WidgetTester tester) async {
    await tester.pumpWidget(createTestApp());

    // Verify we are on Login Screen
    expect(find.byType(LoginScreen), findsOneWidget);

    // Click Guest Mode
    final guestModeText = '${'nav.dashboard'.tr()} (Guest Mode)';
    await tester.tap(find.text(guestModeText));
    await tester.pump();
    await tester.pumpAndSettle();

    // Verify we are on Home Page
    expect(find.byType(HomeScreen), findsOneWidget);
    expect(find.text('app.title'.tr()), findsOneWidget);
  });
}
