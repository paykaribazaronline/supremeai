# 📄 ফাইল: apps/mobile/lib/src/shell/auth_state_shell.dart

**প্রকার:** .dart  
**সাইজ:** 1,260 বাইট  
**আপডেট:** 2026-07-11T13:28:09.100871

---

## কোড

```dart
import 'package:flutter/material.dart';

// Represents the states from our Zod AuthStateEnum
enum AuthStatus {
  uninitialized,
  loggedOut,
  loggedIn,
}

class AuthStateShell extends StatelessWidget {
  final AuthStatus authStatus;
  final Widget splashScreen;
  final Widget loginScreen;
  final Widget dashboardScreen;

  const AuthStateShell({
    Key? key,
    required this.authStatus,
    required this.splashScreen,
    required this.loginScreen,
    required this.dashboardScreen,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // This is the core state machine routing logic
    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 300),
      child: _getScreenForStatus(),
    );
  }

  Widget _getScreenForStatus() {
    switch (authStatus) {
      case AuthStatus.uninitialized:
        return KeyedSubtree(
          key: const ValueKey('splash'),
          child: splashScreen,
        );
      case AuthStatus.loggedOut:
        return KeyedSubtree(
          key: const ValueKey('login'),
          child: loginScreen,
        );
      case AuthStatus.loggedIn:
        return KeyedSubtree(
          key: const ValueKey('dashboard'),
          child: dashboardScreen,
        );
    }
  }
}

```