// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:supremeai_admin/config/app_routes.dart';
import 'package:supremeai_admin/main.dart';
import 'package:supremeai_admin/models/models.dart';
import 'package:supremeai_admin/providers/auth_provider.dart';
import 'package:supremeai_admin/providers/projects_provider.dart';
import 'package:supremeai_admin/providers/metrics_provider.dart';
import 'package:supremeai_admin/providers/theme_provider.dart';

void main() {
  Widget buildApp() {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => ThemeProvider()),
        ChangeNotifierProvider(create: (_) => ProjectsProvider()),
        ChangeNotifierProvider(create: (_) => MetricsProvider()),
      ],
      child: const SupremeAIAdminApp(),
    );
  }

  testWidgets('SupremeAI Admin App smoke test', (WidgetTester tester) async {
    // Build app under the same provider tree used in production.
    await tester.pumpWidget(buildApp());

    // Advance beyond splash delay so no timer remains pending at test teardown.
    await tester.pump(const Duration(seconds: 3));
    await tester.pump();

    // Verify that app launches successfully and displays expected UI
    // (Either shows MaterialApp widget or routing based on initial state)
    expect(find.byType(MaterialApp), findsOneWidget);
    expect(find.byType(SupremeAIAdminApp), findsOneWidget);
  });

  testWidgets('Named project detail route loads edit screen',
      (WidgetTester tester) async {
    await tester.pumpWidget(buildApp());
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();

    final navigator =
        tester.state<NavigatorState>(find.byType(Navigator).first);
    final project = Project(
      id: 'p-1',
      name: 'Route Test Project',
      description: 'Verify named route navigation',
      status: 'PUSHED_TO_REPO',
      templateType: 'REACT',
      repoUrl: 'https://github.com/example/route-test-project',
      repoBranch: 'main',
      progress: 100,
      fileCount: 24,
      pushed: true,
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 1),
    );

    navigator.pushNamed(AppRoutes.projectDetail, arguments: project);
    await tester.pumpAndSettle();

    expect(find.text('প্রজেক্ট সম্পাদনা'), findsOneWidget);
    expect(find.text('Project ID'), findsOneWidget);
  });

  testWidgets('Unknown route falls back to login screen',
      (WidgetTester tester) async {
    await tester.pumpWidget(buildApp());
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();

    final navigator =
        tester.state<NavigatorState>(find.byType(Navigator).first);
    navigator.pushNamed('/not-a-real-route');
    await tester.pumpAndSettle();

    expect(find.text('অ্যাডমিন ম্যানেজমেন্ট পোর্টাল'), findsOneWidget);
  });
}
