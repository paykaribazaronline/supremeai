import 'package:flutter/material.dart';

Widget buildAdminSurface({
  required String dashboardUrl,
  required bool isTestMode,
}) {
  return _AdminFallback(url: dashboardUrl, isTestMode: isTestMode);
}

class _AdminFallback extends StatelessWidget {
  final String url;
  final bool isTestMode;

  const _AdminFallback({
    required this.url,
    required this.isTestMode,
  });

  @override
  Widget build(BuildContext context) {
    final message = isTestMode
        ? 'Unified admin surface is stubbed in widget tests.'
        : 'This platform cannot render the unified admin surface.';

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.admin_panel_settings, size: 64),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              url,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.blueGrey),
            ),
          ],
        ),
      ),
    );
  }
}