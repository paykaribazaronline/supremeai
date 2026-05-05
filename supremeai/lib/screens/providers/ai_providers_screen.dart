import 'package:flutter/material.dart';

class AiProvidersScreen extends StatelessWidget {
  const AiProvidersScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final providers = [
      {'name': 'OpenAI', 'status': 'Online', 'model': 'gpt-4-turbo'},
      {'name': 'Google Gemini', 'status': 'Online', 'model': 'gemini-1.5-pro'},
      {'name': 'Anthropic', 'status': 'Online', 'model': 'claude-3-opus'},
      {'name': 'Groq', 'status': 'Online', 'model': 'llama3-70b-8192'},
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('AI Providers'),
      ),
      body: ListView.builder(
        itemCount: providers.length,
        itemBuilder: (context, index) {
          final p = providers[index];
          return ListTile(
            leading: const Icon(Icons.cloud_queue),
            title: Text(p['name']!),
            subtitle: Text('Model: ${p['model']}'),
            trailing: Chip(
              label: Text(p['status']!),
              backgroundColor: Colors.green.shade100,
            ),
          );
        },
      ),
    );
  }
}
