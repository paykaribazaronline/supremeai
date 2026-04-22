import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'providers/auth_provider.dart';
import 'providers/settings_provider.dart';
import 'providers/orchestration_provider.dart';
import 'screens/login_screen.dart';
import 'screens/dashboard/home_screen.dart';

void main() {
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => SettingsProvider()),
        ChangeNotifierProvider(create: (_) => OrchestrationProvider()),
      ],
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      container: true,
      label: 'SupremeAI Application',
      child: MaterialApp(
        title: 'SupremeAI',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
          useMaterial3: true,
        ),
        home: Consumer<AuthProvider>(
          builder: (context, auth, _) {
            if (auth.status == AuthStatus.authenticated || auth.status == AuthStatus.guest) {
              return const HomeScreen();
            }
            return const LoginScreen();
          },
        ),
      ),
    );
  }
}

