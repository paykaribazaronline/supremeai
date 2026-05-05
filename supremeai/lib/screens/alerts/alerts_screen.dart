import 'package:flutter/material.dart';

class AlertsScreen extends StatelessWidget {
  const AlertsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('System Alerts')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: const [
          Card(
            child: ListTile(
              leading: Icon(Icons.error, color: Colors.red),
              title: Text('API Rate Limit Hit'),
              subtitle: Text('OpenAI API quota exceeded for the current hour.'),
              trailing: Text('5m ago'),
            ),
          ),
          Card(
            child: ListTile(
              leading: Icon(Icons.warning, color: Colors.orange),
              title: Text('Slow Response Time'),
              subtitle: Text('Backend latency higher than 500ms.'),
              trailing: Text('12m ago'),
            ),
          ),
        ],
      ),
    );
  }
}
