import 'package:flutter/material.dart';

class QuotaScreen extends StatelessWidget {
  const QuotaScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tiers & Quotas')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Current Usage', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            const LinearProgressIndicator(value: 0.65, minHeight: 10),
            const SizedBox(height: 5),
            const Text('6,500 / 10,000 requests used'),
            const SizedBox(height: 20),
            const Text('Your Plan: Enterprise AI', style: TextStyle(fontSize: 16, color: Colors.blue)),
            const Divider(height: 32),
            Expanded(
              child: ListView(
                children: const [
                  ListTile(
                    title: Text('API Rate Limit'),
                    subtitle: Text('100 requests per minute'),
                    trailing: Icon(Icons.speed),
                  ),
                  ListTile(
                    title: Text('Max Projects'),
                    subtitle: Text('Unlimited'),
                    trailing: Icon(Icons.inventory),
                  ),
                  ListTile(
                    title: Text('Concurrent Agents'),
                    subtitle: Text('50 Active Agents'),
                    trailing: Icon(Icons.bolt),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
