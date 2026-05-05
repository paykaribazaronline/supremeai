import 'package:flutter/material.dart';

class AnalyticsScreen extends StatelessWidget {
  const AnalyticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Metrics & Alerts'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('System Health', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            _buildMetricCard('CPU Usage', '24%', Colors.blue),
            _buildMetricCard('Memory Usage', '1.2 GB / 4 GB', Colors.green),
            _buildMetricCard('API Latency', '120ms', Colors.orange),
            const SizedBox(height: 20),
            const Text('Recent Alerts', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            _buildAlertTile('High Memory Warning', 'System memory usage exceeded 80%', '10 mins ago', Colors.red),
            _buildAlertTile('New AI Provider Connected', 'Groq Llama-3 now available', '1 hour ago', Colors.green),
          ],
        ),
      ),
    );
  }

  Widget _buildMetricCard(String title, String value, Color color) {
    return Card(
      child: ListTile(
        title: Text(title),
        trailing: Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 18)),
      ),
    );
  }

  Widget _buildAlertTile(String title, String desc, String time, Color color) {
    return Card(
      child: ListTile(
        leading: Icon(Icons.warning, color: color),
        title: Text(title),
        subtitle: Text(desc),
        trailing: Text(time, style: const TextStyle(fontSize: 12)),
      ),
    );
  }
}
