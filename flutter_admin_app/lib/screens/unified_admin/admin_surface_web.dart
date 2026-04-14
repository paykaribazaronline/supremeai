import 'package:web/web.dart' as web;

import 'package:flutter/material.dart';

Widget buildAdminSurface({
  required String dashboardUrl,
  required bool isTestMode,
}) {
  if (!isTestMode) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (web.window.location.href != dashboardUrl) {
        web.window.location.replace(dashboardUrl);
      }
    });
  }

  return Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const CircularProgressIndicator(),
          const SizedBox(height: 16),
          Text(
            isTestMode
                ? 'Unified admin surface test mode.'
                : 'Redirecting to the full admin dashboard...',
            textAlign: TextAlign.center,
          ),
        ],
      ),
    ),
  );
}
