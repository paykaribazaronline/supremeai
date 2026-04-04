import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../../config/environment.dart';
import 'admin_surface_stub.dart'
    if (dart.library.html) 'admin_surface_web.dart'
    if (dart.library.io) 'admin_surface_mobile.dart';

class UnifiedAdminScreen extends StatelessWidget {
  const UnifiedAdminScreen({Key? key}) : super(key: key);

  static const bool _isWidgetTest = bool.fromEnvironment('FLUTTER_TEST');

  String _resolveDashboardUrl() {
    if (kIsWeb) {
      final current = Uri.base;
      final isLocalhost = current.host == 'localhost' || current.host == '127.0.0.1';
      if (isLocalhost) {
        return '${current.origin}/admin.html';
      }
    }

    return '${Environment.baseUrl}/admin.html';
  }

  @override
  Widget build(BuildContext context) {
    final dashboardUrl = _resolveDashboardUrl();

    return Scaffold(
      body: buildAdminSurface(
        dashboardUrl: dashboardUrl,
        isTestMode: _isWidgetTest,
      ),
    );
  }
}