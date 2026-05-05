import 'package:flutter/material.dart';

class VpnScreen extends StatelessWidget {
  const VpnScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('VPN Management')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.vpn_lock, size: 64, color: Colors.blue),
            const SizedBox(height: 16),
            const Text('Secure Gateway for AI Operations', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const Padding(
              padding: EdgeInsets.all(16.0),
              child: Text(
                'Monitor and manage encrypted tunnels used by AI agents for cross-region data synchronization.',
                textAlign: TextAlign.center,
              ),
            ),
            ElevatedButton(
              onPressed: () {},
              child: const Text('Connect to Secure Node'),
            ),
          ],
        ),
      ),
    );
  }
}
