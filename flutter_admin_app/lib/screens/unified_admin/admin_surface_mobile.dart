import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

Widget buildAdminSurface({
  required String dashboardUrl,
  required bool isTestMode,
}) {
  if (isTestMode || WebViewPlatform.instance == null) {
    return Center(
      child: Text(
        'Unified admin surface test mode: $dashboardUrl',
        textAlign: TextAlign.center,
      ),
    );
  }

  return _MobileAdminSurface(dashboardUrl: dashboardUrl);
}

class _MobileAdminSurface extends StatefulWidget {
  final String dashboardUrl;

  const _MobileAdminSurface({required this.dashboardUrl});

  @override
  State<_MobileAdminSurface> createState() => _MobileAdminSurfaceState();
}

class _MobileAdminSurfaceState extends State<_MobileAdminSurface> {
  late final WebViewController _controller;
  int _progress = 0;

  @override
  void initState() {
    super.initState();
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onProgress: (value) {
            if (!mounted) {
              return;
            }
            setState(() {
              _progress = value;
            });
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.dashboardUrl));
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        WebViewWidget(controller: _controller),
        if (_progress < 100) const LinearProgressIndicator(minHeight: 3),
      ],
    );
  }
}
