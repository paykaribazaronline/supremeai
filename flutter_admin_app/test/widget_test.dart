// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:supremeai_admin/main.dart';
import 'package:supremeai_admin/providers/auth_provider.dart';
import 'package:supremeai_admin/providers/projects_provider.dart';
import 'package:supremeai_admin/providers/metrics_provider.dart';

void main() {
  testWidgets('SupremeAI Admin App smoke test', (WidgetTester tester) async {
    // Build app under the same provider tree used in production.
    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider(create: (_) => AuthProvider()),
          ChangeNotifierProvider(create: (_) => ProjectsProvider()),
          ChangeNotifierProvider(create: (_) => MetricsProvider()),
        ],
        child: const SupremeAIAdminApp(),
      ),
    );

    // Advance beyond splash delay so no timer remains pending at test teardown.
    await tester.pump(const Duration(seconds: 3));
    await tester.pump();

    // Verify that app launches successfully and displays expected UI
    // (Either shows MaterialApp widget or routing based on initial state)
    expect(find.byType(MaterialApp), findsOneWidget);
    expect(find.byType(SupremeAIAdminApp), findsOneWidget);
  });
}
