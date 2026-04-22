import 'package:flutter/material.dart';

class LearningScreen extends StatelessWidget {
  const LearningScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('System Learning')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
          const Text('Learning Progress', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 20),
          _buildMetricCard('Techniques Learned', '1,245', Icons.school, Colors.blue),
          _buildMetricCard('Research Tasks', '89', Icons.science, Colors.purple),
          _buildMetricCard('Optimization Gain', '+12.5%', Icons.trending_up, Colors.green),
          const SizedBox(height: 20),
          const Text('Critical Research Controls', style: TextStyle(fontWeight: FontWeight.bold)),
          SwitchListTile(
            title: const Text('Autonomous Research'),
            value: true,
            onChanged: (val) {},
          ),
          SwitchListTile(
            title: const Text('Public Data Sync'),
            value: false,
            onChanged: (val) {},
          ),
          ],
        ),
      ),
    );
  }

  Widget _buildMetricCard(String title, String value, IconData icon, Color color) {
    return Card(
      child: ListTile(
        leading: Icon(icon, color: color),
        title: Text(title),
        trailing: Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
      ),
    );
  }
}
