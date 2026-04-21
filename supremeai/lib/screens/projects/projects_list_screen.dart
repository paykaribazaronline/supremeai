import 'package:flutter/material.dart';

class ProjectsListScreen extends StatelessWidget {
  const ProjectsListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final projects = [
      {'name': 'TaskMaster', 'status': 'Generated', 'date': '2024-05-20'},
      {'name': 'HealthSync', 'status': 'Improving', 'date': '2024-05-18'},
      {'name': 'EcoTracker', 'status': 'Deployed', 'date': '2024-05-15'},
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('My Projects'),
      ),
      body: ListView.builder(
        itemCount: projects.length,
        itemBuilder: (context, index) {
          final p = projects[index];
          return ListTile(
            leading: const Icon(Icons.folder_shared),
            title: Text(p['name']!),
            subtitle: Text('Last Activity: ${p['date']}'),
            trailing: Chip(
              label: Text(p['status']!),
            ),
            onTap: () {
              // Navigate to project details
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        child: const Icon(Icons.add),
      ),
    );
  }
}
