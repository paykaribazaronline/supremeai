import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:firebase_core/firebase_core.dart';
import 'config/app_routes.dart';
import 'config/constants.dart';
import 'firebase_options.dart';
import 'models/models.dart';
import 'providers/auth_provider.dart';
import 'providers/projects_provider.dart';
import 'providers/metrics_provider.dart';
import 'providers/theme_provider.dart';
import 'screens/auth/login_screen.dart';
import 'screens/auth/register_screen.dart';
import 'screens/projects/project_detail_screen.dart';
import 'screens/unified_admin/unified_admin_screen.dart';
import 'screens/admin/user_management_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialise Firebase (required before firebase_auth is used)
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
  
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (context) => AuthProvider(),
        ),
        ChangeNotifierProvider(
          create: (context) => ProjectsProvider(),
        ),
        ChangeNotifierProvider(
          create: (context) => MetricsProvider(),
        ),
        ChangeNotifierProvider(
          create: (context) => ThemeProvider(),
        ),
      ],
      child: const SupremeAIAdminApp(),
    ),
  );
}

class SupremeAIAdminApp extends StatelessWidget {
  const SupremeAIAdminApp({Key? key}) : super(key: key);

  Widget _dashboardScreen(BuildContext context) {
    return const UnifiedAdminScreen();
  }

  Route<dynamic>? _onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case AppRoutes.projectDetail:
        final project = settings.arguments as Project?;
        return MaterialPageRoute(
          settings: settings,
          builder: (_) => ProjectDetailScreen(project: project),
        );
      default:
        return null;
    }
  }

  Route<dynamic> _onUnknownRoute(RouteSettings settings) {
    return MaterialPageRoute(
      settings: settings,
      builder: (_) => const LoginScreen(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<ThemeProvider>(
      builder: (context, themeProvider, child) {
        return MaterialApp(
          title: AppConstants.appName,
          theme: ThemeData(
            useMaterial3: true,
            colorScheme: ColorScheme.fromSeed(
              seedColor: const Color(AppConstants.primaryColor),
              brightness: Brightness.light,
            ),
            appBarTheme: const AppBarTheme(
              elevation: 0,
              centerTitle: true,
            ),
            inputDecorationTheme: InputDecorationTheme(
              filled: true,
              fillColor: const Color(AppConstants.backgroundColor),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
                borderSide: const BorderSide(color: Colors.transparent),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
                borderSide: const BorderSide(color: Colors.transparent),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
                borderSide: const BorderSide(
                  color: Color(AppConstants.primaryColor),
                  width: 2,
                ),
              ),
              contentPadding: const EdgeInsets.symmetric(
                horizontal: AppConstants.paddingMedium,
                vertical: AppConstants.paddingMedium,
              ),
            ),
            elevatedButtonTheme: ElevatedButtonThemeData(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(AppConstants.primaryColor),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(
                  horizontal: AppConstants.paddingLarge,
                  vertical: AppConstants.paddingMedium,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
                ),
              ),
            ),
          ),
          darkTheme: ThemeData(
            useMaterial3: true,
            brightness: Brightness.dark,
            colorScheme: ColorScheme.fromSeed(
              seedColor: const Color(AppConstants.primaryColor),
              brightness: Brightness.dark,
            ),
          ),
          themeMode: themeProvider.themeMode,
          home: const SplashScreen(),
          routes: {
            AppRoutes.login: (context) => const LoginScreen(),
            AppRoutes.register: (context) => const RegisterScreen(),
            AppRoutes.home: _dashboardScreen,
            AppRoutes.projects: _dashboardScreen,
            AppRoutes.projectNew: _dashboardScreen,
            AppRoutes.metrics: _dashboardScreen,
            AppRoutes.settings: _dashboardScreen,
            AppRoutes.learning: _dashboardScreen,
            AppRoutes.teaching: _dashboardScreen,
            AppRoutes.aiProviders: _dashboardScreen,
            AppRoutes.selfExtension: _dashboardScreen,
            AppRoutes.alertsLogs: _dashboardScreen,
            AppRoutes.adminControl: _dashboardScreen,
            AppRoutes.consensus: _dashboardScreen,
            AppRoutes.gitOps: _dashboardScreen,
            AppRoutes.headlessBrowser: _dashboardScreen,
            AppRoutes.chatHistory: _dashboardScreen,
            AppRoutes.systemLearning: _dashboardScreen,
            AppRoutes.quota: _dashboardScreen,
            AppRoutes.vpn: _dashboardScreen,
            AppRoutes.resilience: _dashboardScreen,
            AppRoutes.mlIntelligence: _dashboardScreen,
            AppRoutes.notifications: _dashboardScreen,
            AppRoutes.analytics: _dashboardScreen,
            AppRoutes.decisionHistory: _dashboardScreen,
            AppRoutes.phases: _dashboardScreen,
            AppRoutes.tracing: _dashboardScreen,
            AppRoutes.offlineChat: _dashboardScreen,
            AppRoutes.userManagement: (context) => const UserManagementScreen(),
          },
          onGenerateRoute: _onGenerateRoute,
          onUnknownRoute: _onUnknownRoute,
          debugShowCheckedModeBanner: false,
        );
      },
    );
  }
}

class SplashScreen extends StatefulWidget {
  const SplashScreen({Key? key}) : super(key: key);

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _checkLoginStatus();
  }

  Future<void> _checkLoginStatus() async {
    // Simulate splash delay
    await Future.delayed(const Duration(seconds: 2));

    if (!mounted) return;

    final isLoggedIn = await context.read<AuthProvider>().checkLoginStatus();

    if (!mounted) {
      return;
    }

    Navigator.of(context).pushReplacementNamed(
      isLoggedIn ? AppRoutes.home : AppRoutes.login,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(AppConstants.primaryColor),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 100,
              height: 100,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Icon(
                Icons.admin_panel_settings,
                size: 60,
                color: Color.fromARGB(255, 59, 130, 246),
              ),
            ),
            const SizedBox(height: AppConstants.paddingLarge),
            const Text(
              AppConstants.appName,
              style: TextStyle(
                color: Colors.white,
                fontSize: AppConstants.titleFontSize,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: AppConstants.paddingSmall),
            const Text(
              'অ্যাডমিন ম্যানেজমেন্ট সিস্টেম',
              style: TextStyle(
                color: Colors.white70,
                fontSize: AppConstants.bodyFontSize,
              ),
            ),
            const SizedBox(height: AppConstants.paddingXSmall),
            const Text(
              '(লোড হচ্ছে, অপেক্ষা করুন...)',
              style: TextStyle(
                color: Colors.white54,
                fontSize: AppConstants.captionFontSize,
              ),
            ),
            const SizedBox(height: AppConstants.paddingXLarge),
            const CircularProgressIndicator(
              valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
            ),
          ],
        ),
      ),
    );
  }
}
