# 📄 ফাইল: apps/mobile/lib/screens/dashboard/main_shell.dart

**প্রকার:** .dart  
**সাইজ:** 1,869 বাইট  
**আপডেট:** 2026-07-11T13:38:55.808377

---

## কোড

```dart
import 'package:flutter/material.dart';
import '../../widgets/supreme_bottom_nav.dart';
import '../../theme/app_theme.dart';
// Note: Import your actual screens here. Using placeholders for now.
import 'dashboard_screen.dart';
// import '../analytics/analytics_screen.dart';
// import '../chat/agent_chat_screen.dart';
// import '../api/api_keys_screen.dart';
import '../settings_screen.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _currentIndex = 0;
  late PageController _pageController;

  @override
  void initState() {
    super.initState();
    _pageController = PageController(initialPage: _currentIndex);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onTabTapped(int index) {
    setState(() {
      _currentIndex = index;
    });
    _pageController.jumpToPage(index);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: SupremeColors.bgVoid,
      body: PageView(
        controller: _pageController,
        physics: const NeverScrollableScrollPhysics(), // Disable swipe to change tab
        children: [
          const DashboardScreen(),
          // Placeholder for missing screens, using Center text for now until they are integrated
          const Center(child: Text('Analytics', style: TextStyle(color: Colors.white))),
          const Center(child: Text('Chat', style: TextStyle(color: Colors.white))),
          const Center(child: Text('API Keys', style: TextStyle(color: Colors.white))),
          const SettingsScreen(),
        ],
      ),
      bottomNavigationBar: SupremeBottomNav(
        currentIndex: _currentIndex,
        onTabTapped: _onTabTapped,
      ),
    );
  }
}

```