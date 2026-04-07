import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:firebase_core/firebase_core.dart';
import 'config/app_routes.dart';
import 'config/constants.dart';
import 'firebase_options.dart';
import 'models/models.dart';
import 'services/storage_service.dart';
import 'providers/auth_provider.dart';
import 'providers/projects_provider.dart';
import 'providers/metrics_provider.dart';
import 'screens/auth/login_screen.dart';
import 'screens/auth/register_screen.dart';
import 'screens/projects/project_detail_screen.dart';
import 'screens/projects/projects_list_screen.dart';
import 'screens/metrics_screen.dart';
import 'screens/settings_screen.dart';
import 'screens/unified_admin/unified_admin_screen.dart';
import 'screens/learning/learning_screen.dart';
import 'screens/teaching/teaching_screen.dart';
import 'screens/providers/ai_providers_screen.dart';
import 'screens/extension/self_extension_screen.dart';
import 'screens/alerts/alerts_logs_screen.dart';
import 'screens/admin/admin_control_screen.dart';
import 'screens/consensus/consensus_screen.dart';
import 'screens/git/git_ops_screen.dart';
import 'screens/quota/quota_screen.dart';
import 'screens/vpn/vpn_screen.dart';
import 'screens/resilience/resilience_screen.dart';
import 'screens/ml/ml_intelligence_screen.dart';
import 'screens/notifications/notifications_screen.dart';
import 'screens/analytics/analytics_screen.dart';
import 'screens/decisions/decision_history_screen.dart';
import 'screens/phases/phases_screen.dart';
import 'screens/tracing/tracing_screen.dart';
import 'screens/chat/offline_chat_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialise Firebase (required before firebase_auth is used)
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );

  // Initialize storage service
  final storageService = StorageService();
  await storageService.init();
  
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
      ],
      child: const SupremeAIAdminApp(),
    ),
  );
}

class SupremeAIAdminApp extends StatelessWidget {
  const SupremeAIAdminApp({Key? key}) : super(key: key);

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
    return MaterialApp(
      title: AppConstants.appName,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: Color(AppConstants.primaryColor),
          brightness: Brightness.light,
        ),
        appBarTheme: const AppBarTheme(
          elevation: 0,
          centerTitle: true,
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: Color(AppConstants.backgroundColor),
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
            borderSide: BorderSide(
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
            backgroundColor: Color(AppConstants.primaryColor),
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
          seedColor: Color(AppConstants.primaryColor),
          brightness: Brightness.dark,
        ),
      ),
      themeMode: ThemeMode.light,
      home: const SplashScreen(),
      routes: {
        AppRoutes.login: (context) => const LoginScreen(),
        AppRoutes.register: (context) => const RegisterScreen(),
        AppRoutes.home: (context) => const UnifiedAdminScreen(),
        AppRoutes.projects: (context) => const ProjectsListScreen(),
        AppRoutes.projectNew: (context) => const ProjectDetailScreen(),
        AppRoutes.metrics: (context) => const MetricsScreen(),
        AppRoutes.settings: (context) => const SettingsScreen(),
        AppRoutes.learning: (context) => const LearningScreen(),
        AppRoutes.teaching: (context) => const TeachingScreen(),
        AppRoutes.aiProviders: (context) => const AIProvidersScreen(),
        AppRoutes.selfExtension: (context) => const SelfExtensionScreen(),
        AppRoutes.alertsLogs: (context) => const AlertsLogsScreen(),
        AppRoutes.adminControl: (context) => const AdminControlScreen(),
        AppRoutes.consensus: (context) => const ConsensusScreen(),
        AppRoutes.gitOps: (context) => const GitOpsScreen(),
        AppRoutes.quota: (context) => const QuotaScreen(),
        AppRoutes.vpn: (context) => const VpnScreen(),
        AppRoutes.resilience: (context) => const ResilienceScreen(),
        AppRoutes.mlIntelligence: (context) => const MlIntelligenceScreen(),
        AppRoutes.notifications: (context) => const NotificationsScreen(),
        AppRoutes.analytics: (context) => const AnalyticsScreen(),
        AppRoutes.decisionHistory: (context) => const DecisionHistoryScreen(),
        AppRoutes.phases: (context) => const PhasesScreen(),
        AppRoutes.tracing: (context) => const TracingScreen(),
        AppRoutes.offlineChat: (context) => const OfflineChatScreen(),
      },
      onGenerateRoute: _onGenerateRoute,
      onUnknownRoute: _onUnknownRoute,
      debugShowCheckedModeBanner: false,
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

    Navigator.of(context).pushReplacementNamed(AppRoutes.home);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Color(AppConstants.primaryColor),
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
