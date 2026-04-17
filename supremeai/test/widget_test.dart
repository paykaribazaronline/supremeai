import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:supremeai/main.dart';
import 'package:supremeai/providers/auth_provider.dart';
import 'package:supremeai/screens/login_screen.dart';

void main() {
  testWidgets('Initial screen is LoginScreen when not authenticated', (WidgetTester tester) async {
    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => AuthProvider(),
        child: const MyApp(),
      ),
    );

    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.text('SupremeAI'), findsOneWidget);
    expect(find.text('লগইন করুন'), findsOneWidget);
  });

  testWidgets('Clicking Guest Mode navigates to MyHomePage', (WidgetTester tester) async {
    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => AuthProvider(),
        child: const MyApp(),
      ),
    );

    // Verify we are on Login Screen
    expect(find.byType(LoginScreen), findsOneWidget);

    // Click Guest Mode
    await tester.tap(find.text('게স্ট হিসেবে ব্যবহার করুন (Guest Mode)'));
    await tester.pumpAndSettle();

    // Verify we are on Home Page
    expect(find.byType(MyHomePage), findsOneWidget);
    expect(find.text('SupremeAI Home'), findsOneWidget);
  });
}
